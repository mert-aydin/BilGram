/*
Wire
Copyright (C) 2016 Wire Swiss GmbH
<p>
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
<p>
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
<p>
You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
<p>
Please see
https://github.com/wireapp/wire-android/blob/master/wire-ui/src/main/java/com/waz/zclient/ui/views/OnDoubleClickListener.java
*/

package com.mert.bilgram;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewConfiguration;

public abstract class OnDoubleClickListener implements View.OnClickListener {
    private final int doubleClickTimeout;
    private Handler handler;

    private long firstClickTime;

    OnDoubleClickListener() {
        doubleClickTimeout = ViewConfiguration.getDoubleTapTimeout();
        firstClickTime = 0L;
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onClick(final View v) {
        long now = System.currentTimeMillis();

        if (now - firstClickTime < doubleClickTimeout) {
            handler.removeCallbacksAndMessages(null);
            firstClickTime = 0L;
            onDoubleClick(v);
        } else {
            firstClickTime = now;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onSingleClick(v);
                    firstClickTime = 0L;
                }
            }, doubleClickTimeout);
        }
    }

    public abstract void onDoubleClick(View v);

    public abstract void onSingleClick(View v);

    public void reset() {
        handler.removeCallbacksAndMessages(null);
    }
}