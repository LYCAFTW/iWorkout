package com.iotlyca.util;

import android.graphics.Bitmap;

/**
 * Created by guangyang on 17/5/6.
 */

public class Friend {
    private String image;
    private String name;
    private String message;

    public Friend(String _image, String _name, String _message) {
        this.image = _image;
        this.name = _name;
        this.message = _message;
    }

    public String getImage() { return this.image; }

    public String getName() { return this.name; }

    public String getMessage() { return this.message; }
}
