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

/**
 * (c) 2017, Space-Time Insight
 */
public enum MarkerType {
    DEVICE(0),
    CHANNEL(1),
    THING(2),
    UNKNOWN(99);

    private int value;

    MarkerType(int type) {
        value = type;
    }

    public int getValue() {
        return value;
    }

    public static MarkerType discoverMatchingEnum(long arg) {
        switch( (int) arg ) {
            case 0:     return MarkerType.DEVICE;
            case 1:     return MarkerType.CHANNEL;
            case 2:     return MarkerType.THING;
            default:    return MarkerType.UNKNOWN;
        }
    }
}
