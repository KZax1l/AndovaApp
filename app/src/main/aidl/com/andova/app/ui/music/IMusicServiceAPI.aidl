// IMusicServiceAPI.aidl
package com.andova.app.ui.music;

// Declare any non-default types here with import statements

interface IMusicServiceAPI {
    void open(in long [] list, int position, long sourceId);
    void play();
    void stop();
    void pause();
    void previous();
    void next();
}
