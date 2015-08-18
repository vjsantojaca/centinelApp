package app.vjsantojaca.merinosa.com.centinela;

import android.content.Context;
import android.content.ContextWrapper;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/*
* Developer Víctor Santoja
 * Centinela App pertenece al proyecto Centinela
*/
public class AudioRecorder
{
    private final MediaRecorder recorder = new MediaRecorder();
    private String path;

    /**
     * Creates a new audio recording at the given path (relative to root of SD card).
     */
    public AudioRecorder(String path)
    {
        this.path = sanitizePath(path);
    }

    /**
     * Creates a new audio recording.
     */
    public AudioRecorder() { }

    private String sanitizePath(String path)
    {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.contains(".")) {
            path += ".3gp";
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath() + path;
    }

    /**
     * Starts a new recording.
     */
    public void start() throws IOException
    {
        if( path != null )
        {
            String state = android.os.Environment.getExternalStorageState();
            if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
                throw new IOException("SD Card is not mounted.  It is " + state + ".");
            }

            // make sure the directory we plan to store the recording in exists
            File directory = new File(path).getParentFile();
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Path to file could not be created.");
            }
        } else
        {
            ContextWrapper cw = new ContextWrapper(App.getAppContext());
            File dir = cw.getDir("sound_dir", Context.MODE_PRIVATE);
            File mypath=new File(dir,"sound_" + (new Date()).getTime() + ".3gp");
            path = mypath.getPath();
        }

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(path);
        recorder.prepare();
        recorder.start();
    }

    /**
     * Stops a recording that has been previously started.
     */
    public void stop() throws IOException {
        recorder.stop();
        recorder.release();
    }
}