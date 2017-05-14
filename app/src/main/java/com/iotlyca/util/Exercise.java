package com.iotlyca.util;

/**
 * Created by guangyang on 17/5/8.
 */

public class Exercise {
    //actSet = ['chest press', 'seated row', 'leg press', 'abdominal', 'bicep curl', 'counter balance smith', 'tricep press', 'leg extension', 'hyperextension']
    private String exerciseName;
    private int imageSrc;

    public Exercise(String _exerciseName, int _imageSrc) {
        exerciseName = _exerciseName;
        imageSrc = _imageSrc;

    }

    public String getName() {
        return exerciseName;
    }

    public int getImage() {
        return imageSrc;
    }
}
