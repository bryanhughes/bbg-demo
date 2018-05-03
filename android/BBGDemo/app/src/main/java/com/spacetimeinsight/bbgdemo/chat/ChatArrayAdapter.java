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
package com.spacetimeinsight.bbgdemo.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.spacetimeinsight.bbgdemo.BBGDemoApplication;
import com.spacetimeinsight.bbgdemo.R;
import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.Channel;
import com.spacetimeinsight.nucleuslib.NucleusException;
import com.spacetimeinsight.nucleuslib.datamapped.ChannelMessage;
import com.spacetimeinsight.nucleuslib.datamapped.Member;
import com.spacetimeinsight.nucleuslib.datamapped.MimeMessage;
import com.spacetimeinsight.nucleuslib.datamapped.MimePart;
import com.spacetimeinsight.nucleuslib.types.MimeType;
import com.spacetimeinsight.protobuf.nano.MimeMessageProto;

import java.util.List;
import java.util.Locale;

/**
 * (c) 2017, Space-Time Insight
 */
public class ChatArrayAdapter extends ArrayAdapter<ChannelMessage> {
    private BBGDemoApplication bbgDemoApplication;
    private Channel channel;

    void setApplication(BBGDemoApplication app) {
        this.bbgDemoApplication = app;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void clear() {
        this.channel = null;
    }

    MimeMessageProto.MimeMessage makeMessage(String message, byte[] imageData) {
        MimeMessageProto.MimePart mimePart = new MimeMessageProto.MimePart();
        mimePart.contentType = "text/plain";
        byte[] src = message.getBytes();
        byte[] content = new byte[src.length];
        System.arraycopy(src, 0, content, 0, src.length);
        mimePart.content  = content;

        int cnt = (imageData == null ? 1 : 2);

        final MimeMessageProto.MimeMessage mimeMessage = new MimeMessageProto.MimeMessage();
        MimeMessageProto.MimePart[] parts = new MimeMessageProto.MimePart[cnt];
        parts[0] = mimePart;

        if ( imageData != null ) {
            mimePart = new MimeMessageProto.MimePart();
            mimePart.contentType = "image/jpg";
            mimePart.content  = imageData;
            parts[1] = mimePart;
        }

        mimeMessage.parts = parts;
        return mimeMessage;
    }

    @Override
    public int getCount() {
        if ( channel != null ) {
            return channel.getChannelMessageCount();
        }
        else {
            return 0;
        }
    }

    public ChatArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @SuppressLint("RtlHardcoded")
    public View getView(int position, View convertView, ViewGroup parent) {

        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        ChannelMessage message;

        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) bbgDemoApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.content_chat_message, parent, false);
        }

        try {
            message = channel.getChannelMessageItem(position);
            MimeMessage mimeMessage = message.getMimeMessage();
            List<MimePart> parts = mimeMessage.getMimeParts();
            MimePart part = parts.get(0);

            Channel channel = nucleusService.getCurrentChannel();

            TextView chatText = row.findViewById(R.id.chatMessageText);
            ImageView imageView = row.findViewById(R.id.chatProfileImage);
            TextView screenName = row.findViewById(R.id.chatSenderName);
            TextView timestamp = row.findViewById(R.id.chatTimestampField);
            TextView idField = row.findViewById(R.id.chatMessageID);
            ProgressBar progressBar = row.findViewById(R.id.chatProgressSpinner);

            if ( message.isInFlight() ) {
                progressBar.setVisibility(View.VISIBLE);
            }
            else {
                progressBar.setVisibility(View.INVISIBLE);
            }

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

            idField.setText(String.valueOf(message.getOffset()));

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

            if ( part.getContentType().equals(MimeType.TEXT_PLAIN) ) {
                String content = new String(part.getContent());
                chatText.setText(content);
                chatText.setVisibility(View.VISIBLE);
            }
            else {
                chatText.setVisibility(View.INVISIBLE);
            }
        }
        catch (NucleusException e) {
            e.printStackTrace();
            nucleusService.handleOnError(100, "Caught exception - " + e.getLocalizedMessage());
        }

        return row;
    }

    public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }
}
