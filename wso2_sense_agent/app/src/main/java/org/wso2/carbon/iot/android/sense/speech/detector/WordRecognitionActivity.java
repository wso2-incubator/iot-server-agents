package org.wso2.carbon.iot.android.sense.speech.detector;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.wso2.carbon.iot.android.sense.sensordataview.ActivitySelectSensor;
import org.wso2.carbon.iot.android.sense.speech.detector.util.ListeningActivity;
import org.wso2.carbon.iot.android.sense.speech.detector.util.ProcessWords;
import org.wso2.carbon.iot.android.sense.speech.detector.util.VoiceRecognitionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import agent.sense.android.iot.carbon.wso2.org.wso2_senseagent.R;

public class WordRecognitionActivity extends ListeningActivity {
    Button setThreasholdButton;
    Button addWordButton;
    Button removeWordButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_sense_main);
        context = getApplicationContext(); // Needs to be set

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        VoiceRecognitionListener.getInstance().setListener(this); // Here we set the current listener
        addListenerOnSetThreasholdButton();
        addListenerOnAddWordButton();
        addListenerOnRemoveWordButton();
        ProcessWords.setSessionId(UUID.randomUUID().toString());
        FloatingActionButton fbtnSpeechRecongnizer = (FloatingActionButton) findViewById(R.id.sensorChange);
        fbtnSpeechRecongnizer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProcessWords.setSessionId(UUID.randomUUID().toString());
                stopListening();
                Intent intent = new Intent(getApplicationContext(), ActivitySelectSensor.class);
                startActivity(intent);
            }
        });
        startListening(); // starts listening
    }


    @Override
    public void processVoiceCommands(String... voiceCommands) {
        if(voiceCommands==null || voiceCommands.length==0){
            return;
        }
        ProcessWords processWords = new ProcessWords(this);
        processWords.execute(voiceCommands);
        restartListeningService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void addListenerOnSetThreasholdButton() {
        setThreasholdButton = (Button) findViewById(R.id.setThreshold);
        setThreasholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String thresholdString = ((EditText) findViewById(R.id.editThreashold)).getText().toString();
                try{
                    ProcessWords.setThreshold(Integer.parseInt(thresholdString));
                } catch (NumberFormatException e) {
                    Toast.makeText(WordRecognitionActivity.this, "Invalid Threshold - " + thresholdString, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void addListenerOnAddWordButton() {
        addWordButton = (Button) findViewById(R.id.addWord);
        addWordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String word = ((EditText) findViewById(R.id.wordText)).getText().toString();
                ProcessWords.addWord(word);
                Toast.makeText(WordRecognitionActivity.this, word + " is added to the list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addListenerOnRemoveWordButton() {
        removeWordButton = (Button) findViewById(R.id.removeWord);
        removeWordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String word = ((EditText) findViewById(R.id.wordText)).getText().toString();
                Toast.makeText(WordRecognitionActivity.this, word + " is removed from the list", Toast.LENGTH_SHORT).show();
                ProcessWords.removeWord(word);
            }

        });
    }
}
