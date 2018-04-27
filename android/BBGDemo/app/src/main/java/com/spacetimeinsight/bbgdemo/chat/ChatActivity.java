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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ListView;

import com.spacetimeinsight.bbgdemo.BBGDemoApplication;
import com.spacetimeinsight.bbgdemo.R;
import com.spacetimeinsight.nucleus.android.NucleusService;
import com.spacetimeinsight.nucleuslib.ChannelService;
import com.spacetimeinsight.nucleuslib.datamapped.MimeMessage;
import com.spacetimeinsight.nucleuslib.responsehandlers.ChannelPublishMessageResponseHandler;
import com.spacetimeinsight.nucleuslib.types.OperationStatus;
import com.spacetimeinsight.protobuf.nano.MimeMessageProto;

import java.io.IOException;

public class ChatActivity extends AppCompatActivity {
    private static final String LOG_TAG = ChatActivity.class.getName();
    private EditText messageText;
    private ChatArrayAdapter chatArrayAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        BBGDemoApplication app = (BBGDemoApplication) getApplication();
        app.setCurrentActivity(this);

        chatArrayAdapter = app.getChatAdapter();
        chatArrayAdapter.setApplication(app);
        ListView lv = findViewById(R.id.listView1);
        lv.setAdapter(chatArrayAdapter);

        messageText = findViewById(R.id.messageText);
        messageText.setOnKeyListener((v, keyCode, event) -> {
            // If the event is a key-down event on the "enter" button
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Perform action on key press
                MimeMessageProto.MimeMessage mimeMessage = chatArrayAdapter.makeMessage(messageText.getText().toString(), null);
                try {
                    publishMessage(mimeMessage);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                messageText.setText("");
                return true;
            }

            return false;
        });
    }

    /**
     * Publishing a message to Nucleus.
     * It is important to realize that when a message is published to Nucleus. It may actually still be in the internal
     * send queue of the SDK waiting to be sent. The main reason why this would happen is that the phone does not
     * actually have a network connection to send on. The SDK buffers all command messages in order until a connection
     * is re-established.
     *
     * It is important to understand that until the server responds to the publish message command, the real messageID
     * is not know. Message IDs are assigned by the server at the time that the server handled them. This means that
     * until the client SDK get's the response, the ID of the message is unknown. In its stead, the SDK uses the eventID
     * which is a unique monotonic sequence to this SDK that starts at the upper end of the LONG range.
     *
     * From the users experience, they are looking at the chat stream on their device and sending messages, they may
     * not realize they are offline or often on a network where some messages are being received while outgoing
     * message is pending. To resolve this, the ArrayAdapter simply sorts the messages by ascending order from the
     * bottom up. Pending or in-flight messages are assigned a temporary messageID that is very high so it will always
     * be at the bottom until the message gets "fixed up" after the server acknowledges the receipt of it.
     *
     * @param mimeMessage the message to publish
     */
    private void publishMessage(final MimeMessageProto.MimeMessage mimeMessage)
            throws IOException {
        NucleusService nucleusService = BBGDemoApplication.getNucleusService();
        ChannelService channelService = nucleusService.getChannelService();
        String channelRef = nucleusService.getCurrentChannelRef();
        if ( channelRef == null ) {
            BBGDemoApplication app = (BBGDemoApplication) getApplication();
            app.showAlert("Warning", "You are not in an active channel. Please either create or join one.");
            Log.e(LOG_TAG, "Null channelRef - not in an active channel");
        }
        else {
            MimeMessage mimeMsg = new MimeMessage(mimeMessage);
            channelService.publish(channelRef, mimeMsg,
                                   new ChannelPublishMessageResponseHandler() {
                                       @Override
                                       public void onFailure(OperationStatus operationStatus, int statusCode,
                                                             String errorMessage, boolean retryable) {
                                           BBGDemoApplication app = (BBGDemoApplication) getApplication();
                                           app.showAlert("Error", "Failed to publish message");
                                           Log.e(LOG_TAG, "Failed to publish message - " + operationStatus +
                                                          "(" + statusCode + ") " +
                                                          errorMessage);
                                       }

                                       @Override
                                       public void onSuccess(long offset, long eventID) {

                                       }
                                   });
        }
    }
}
