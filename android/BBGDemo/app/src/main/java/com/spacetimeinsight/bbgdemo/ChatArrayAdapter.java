/*
 * Copyright 2017, SpaceTime-Insight, Inc.
 *
 * This code is supplied as an example of how to use the SpaceTime Warp IoT Nucleus SDK. It is
 * intended solely to demonstrate usage of the SDK and its features, and as a learning by example.
 * This code is not intended for production or commercial use as-is.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except
 * in compliance with the License. You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.spacetimeinsight.bbgdemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.Channel;
import com.spacetimeinsight.nucleuslib.Member;
import com.spacetimeinsight.nucleuslib.datamapped.ChannelMessage;
import com.spacetimeinsight.nucleuslib.datamapped.MimeMessage;
import com.spacetimeinsight.nucleuslib.datamapped.MimePart;
import com.spacetimeinsight.protobuf.nano.MimeMessageProto;
import com.spacetimeinsight.protobuf.nano.MimePartProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * (c) 2017, Space-Time Insight
 */
class ChatArrayAdapter extends ArrayAdapter<ChannelMessage> {
    private static final String LOG_TAG = ChatArrayAdapter.class.getName();
    private BBGDemoApplication bbgDemoApplication;

    private Map<Long, ChannelMessage> messageMap = new HashMap<>();
    private List<Long> messageList = new ArrayList<>();

    void setApplication(BBGDemoApplication app) {
        this.bbgDemoApplication = app;
    }

    @Override
    public void add(final ChannelMessage message) {
        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        String channelRef = nucleusService.getCurrentChannelRef();
        if ( channelRef == null ) {
            bbgDemoApplication.showAlert("Warning",
                                         "You are not in an active channel. Please either create or join one.");
            Log.e(LOG_TAG, "Null channelRef - not in an active channel");
        }
        else {
            if (message.getOffset() > 0x0fffffff) {
                return;  //filter out temporary message.
            }
            Log.i(LOG_TAG, "Add message Offset " + message.getMessageOffset());
            // NOTE: We want to use a map for our message storage with a sorted list of positional access. This will
            // allow us to ensure one message per adapter.

            if ( ! messageList.contains(message.getMessageOffset()) ) {
                messageList.add(message.getMessageOffset());
            }
            messageMap.put(message.getMessageOffset(), message);

            Collections.sort(messageList, new Comparator<Long>() {
                @Override
                public int compare(Long lhs, Long rhs) {
                    if ( lhs < rhs ) {
                        return -1;
                    }
                    else if ( lhs > rhs ) {
                        return 1;
                    }
                    else {
                        return 0;
                    }
                }
            });
        }
    }

    @Override
    public void clear() {
        super.clear();
        messageMap.clear();
        messageList.clear();
    }

    MimeMessageProto.MimeMessage makeMessage(String message, byte[] imageData) {
        MimePartProto.MimePart mimePart = new MimePartProto.MimePart();
        mimePart.contentType = "text/plain";
        byte[] src = message.getBytes();
        byte[] content = new byte[src.length];
        System.arraycopy(src, 0, content, 0, src.length);
        mimePart.content  = content;

        int cnt = (imageData == null ? 1 : 2);

        final MimeMessageProto.MimeMessage mimeMessage = new MimeMessageProto.MimeMessage();
        MimePartProto.MimePart[] parts = new MimePartProto.MimePart[cnt];
        parts[0] = mimePart;

        if ( imageData != null ) {
            mimePart = new MimePartProto.MimePart();
            mimePart.contentType = "image/jpg";
            mimePart.content  = imageData;
            parts[1] = mimePart;
        }

        mimeMessage.parts = parts;

        return mimeMessage;
    }

    @Override
    public int getCount() {
        return messageList.size();
    }

    @Override
    public void remove(ChannelMessage message) {
        // This is only to remove the in-flight message that was keyed by the eventID.
        messageList.remove(message.getEventID());
        messageMap.remove(message.getEventID());
    }

    public ChatArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @SuppressLint("RtlHardcoded")
    public View getView(int position, View convertView, ViewGroup parent) {

        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        Long mid = messageList.get(position);
        ChannelMessage message = messageMap.get(mid);

        View row = null;
        try {
            MimeMessage mimeMessage = message.getMimeMessage();
            List<MimePart> parts = mimeMessage.getMimeParts();
            MimePart part = parts.get(0);

            row = convertView;
            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) bbgDemoApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.content_chat_message, parent, false);
            }

            Channel channel = nucleusService.getCurrentChannel();

            TextView chatText = (TextView) row.findViewById(R.id.chatMessageText);
            ImageView imageView = (ImageView) row.findViewById(R.id.chatProfileImage);
            TextView screenName = (TextView) row.findViewById(R.id.chatSenderName);
            TextView timestamp = (TextView) row.findViewById(R.id.chatTimestampField);
            TextView idField = (TextView) row.findViewById(R.id.chatMessageID);

            String deviceID = message.getSenderID();

            Member member = channel.getMember(deviceID);
            screenName.setText(member.getScreenName());

            Bitmap bitmap;
            if ( deviceID.equals("0") ) {
                bitmap = BBGDemoApplication.getSystemImage(bbgDemoApplication.getCurrentActivity());
            }
            else {
                byte[] imageData = member.getProfileImage();
                if ( imageData != null ) {
                    bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                }
                else {
                    bitmap = BitmapFactory.decodeResource(bbgDemoApplication.getResources(), R.mipmap.profile_missing);
                }
            }
            imageView.setImageBitmap(bitmap);

            idField.setText(String.valueOf(message.getMessageOffset()));

            String tsStr;
            long ts = message.getTimestamp();
            if (ts < 100) {
                tsStr = "no timestamp";
            }
            else {
                long interval = (System.currentTimeMillis() / 1000) - ts;
                if (interval > (60 * 60)) {
                    int hours = (int) (interval / 60) / 60;
                    tsStr = String.format(Locale.US, "%d %s ago", hours, (hours > 1 ? "hours" : "hour"));
                } else if (interval > 60) {
                    int minutes = (int) (interval / 60);
                    tsStr = String.format(Locale.US, "%d %s ago", minutes, (minutes > 1 ? "minutes" : "minute"));
                } else {
                    tsStr = "less than a minute ago";
                }
            }
            timestamp.setText(tsStr);

            if ( part.getContentType().equals("text/plain") ) {
                String content = new String(part.getContent());
                chatText.setText(content);
                chatText.setVisibility(View.VISIBLE);
            }
            else {
                chatText.setVisibility(View.INVISIBLE);
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Failed to parse protobuffer. " + e.getLocalizedMessage(), e);
            nucleusService.handleOnError(100, "Failed to parse message.");
        }

        return row;
    }

    public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }
}
