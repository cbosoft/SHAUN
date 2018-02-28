package cbosoft.shaun;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.transition.AutoTransition;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.os.Build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static android.content.ContentValues.TAG;

public class Home extends Activity {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// ATTRIBUTES ///////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // UI
    // Main
    TextView shInfo;
    TextClock shClock, shDate;
    BufferView shSTDOUT;
    InputBuffer shSTDIN;

    // Scenes
    ViewGroup rootScene;
    Scene mainScene, hiddenScene, editorScene;
    Transition transistion = new AutoTransition();
    SceneType sceneType = SceneType.MINIMISED;

    // Shell
    String shPrompt;
    String shCurrentDirectory = Environment.getExternalStorageDirectory().getAbsolutePath(); //Environment.getRootDirectory().toString();
    Boolean redirect = false;
    ArrayList<String> reBuff = new ArrayList<>(0), plcBuff;
    String prevCommand = "";

    Map<String, String> ALIASES;
    Map<String, Integer> input2type;
    Map<String, String> input2andrapp;
    Map<String, String> ignoreapps;
    Map<String, String> manmap;
    Map<String, String> SHVARS;

    SharedPreferences shPref;
    SharedPreferences.Editor shPrefEditor;

    // Editor
    ShaunEditor shEditor;

    TextWatcher tWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            inputTextChanged(s, start, before, count);
        }
    };

    TextWatcher editorWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence s, int i, int i1, int i2) {
            shEditor.edited = true;

            String ss = s.toString();

            if (ss.length() == shEditor.fileBuffer.length()) {
                if (ss.equals(shEditor.fileBuffer)) {
                    shEditor.edited = false;
                }
            }

            shEditor.updateUI();
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    class ShaunEditor{
        // Android Views
        private TextView shedStatus, shedFilePath, shedNotif;
        private ShaunEditorView shedIn;
        private Button saveBtn, quitBtn;

        private String filePath = "", fileBuffer = "", status = "";
        ShaunEditorFileType currentFileType;
        boolean edited = false;

        ShaunEditor(Typeface tf, TextWatcher tw) {

            this.currentFileType = ShaunEditorFileType.PLAINTEXT;

            this.shedStatus = findViewById(R.id.shedStatus);
            this.shedFilePath = findViewById(R.id.shedFilePath);
            this.shedIn = findViewById(R.id.shedIn);
            this.saveBtn = findViewById(R.id.btnSave);
            this.quitBtn = findViewById(R.id.btnQuit);
            this.shedNotif = findViewById(R.id.shedNotification);

            this.shedStatus.setTypeface(tf);
            this.shedFilePath.setTypeface(tf);
            this.shedIn.setTypeface(tf);
            this.saveBtn.setTypeface(tf);
            this.quitBtn.setTypeface(tf);
            this.shedNotif.setTypeface(tf);


            this.shedFilePath.setText(R.string.newFileString);
            this.shedIn.addTextChangedListener(tw);
            this.shedIn.requestFocus();

            this.updateUI();
        }

        int openFileToBuffer(String filePath) {
            String line;
            StringBuilder sb = new StringBuilder();

            this.filePath = filePath;
            shedFilePath.setText(filePath);

            int rv = 0;

            try {
                FileReader fileReader = new FileReader(filePath);
                BufferedReader bufferedReader = new BufferedReader(fileReader);

                while((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
                bufferedReader.close();
            }
            catch(FileNotFoundException ex) {
                Log.d(TAG, "openFileToBuffer: fnfex");
                rv = 1;
            }
            catch(IOException ex) {
                flashMessage("Could not open file.");
                rv = -1;
            }

            fileBuffer = sb.toString();
            Log.d(TAG, "openFileToBuffer: " + sb.toString());
            currentFileType = guessThisFileType();

            setBuffer();
            updateUI();

            return rv;
        }

        void writeBufferToFile() {
            writeBufferToFile(this.filePath);
        }

        void writeBufferToFile(String filePath) {

            if (!checkNGetWritePermission()) {
                flashMessage("Need write permission to save file.");
                return;
            }

            try {
                File f = new File(filePath);
                if (!f.exists()) f.createNewFile();
            }
            catch (IOException ioex) { /* Pass */ }

            try {
                FileWriter fileWriter = new FileWriter(filePath);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                this.fileBuffer = this.shedIn.getText().toString();
                bufferedWriter.write(this.fileBuffer);
                bufferedWriter.flush();
                bufferedWriter.close();
                this.edited = false;
                this.updateUI();
                flashMessage("File saved!");
            }
            catch(FileNotFoundException ex) {
                flashMessage("Could not find file.");
            }
            catch(IOException ex) {
                flashMessage("Could not open file.");
            }
        }

        boolean canQuit() {
            return !edited;
        }

        private ShaunEditorFileType guessThisFileType() {
            return guessFileType(fileBuffer, filePath);
        }

        private ShaunEditorFileType guessFileType(String fileContents, String filePath) {
            int[] likelihoods = new int[]{50, 0, 0, 0, 0, 0, 0, 0, 0};

            int magic_conf = 100, comments_conf = 20, extension_conf = 100;

            String ext;
            if (filePath.contains(".")) {
                ext = filePath.substring(filePath.lastIndexOf(".") + 1);
                Log.d(TAG, "guessFileType: " + ext);
                switch (ext) {
                    case "txt":
                        likelihoods[0] += extension_conf;
                    case "c":
                        likelihoods[1] += extension_conf;
                        break;
                    case "cpp":
                    case "cc":
                        likelihoods[2] += extension_conf;
                        break;
                    case "py":
                    case "pyx":
                        likelihoods[3] += extension_conf;
                        break;
                    case "html":
                    case "yaml":
                    case "yml":
                    case "xml":
                        likelihoods[4] += extension_conf;
                        break;
                    case "json":
                        likelihoods[5] += extension_conf;
                        break;
                    case "sh":
                        likelihoods[6] += extension_conf;
                        break;
                    case "tex":
                        likelihoods[7] += extension_conf;
                        break;
                    case "java":
                        likelihoods[8] += extension_conf;
                        break;
                }
            }

            if (fileContents.startsWith("#")) {
                String magicLine = fileContents.split("\n")[0];

                if (magicLine.endsWith("python") || magicLine.endsWith("python2") || magicLine.endsWith("python3")) {
                    likelihoods[3] += magic_conf;
                }

                if (magicLine.endsWith("bash")) {
                    likelihoods[6] += magic_conf;
                }

                if (magicLine.startsWith("\\documentclass{")) {
                    likelihoods[7] += magic_conf;
                }

                if (magicLine.startsWith("<!")) {
                    likelihoods[4] += magic_conf;
                }
            }

            if (fileContents.contains("//") || fileContents.contains("/*")) {
                // uses 'C' style comments.... probably
                likelihoods[1] += comments_conf;
                likelihoods[2] += comments_conf;
                likelihoods[8] += comments_conf;

                if (fileContents.contains("\n#include")) {
                    // 'C' style import statements
                    likelihoods[1] += comments_conf;
                    likelihoods[2] += comments_conf;
                }
            }

            if (fileContents.contains("\nimport")) {
                // import statement
                likelihoods[3] += comments_conf;
                likelihoods[8] += comments_conf;
            }

            if (fileContents.contains("<") && fileContents.contains("</") && fileContents.contains(">")) {
                // Perhaps some kind of tagging?
                likelihoods[4] += comments_conf;
            }

            int most = 0, at = 0;
            for (int i = 0; i < likelihoods.length; i++) {
                if (likelihoods[i] > most) {
                    most = likelihoods[i];
                    at = i;
                }
            }

            switch (at) {
                case 0:
                    return ShaunEditorFileType.PLAINTEXT;
                case 1:
                    return ShaunEditorFileType.C;
                case 2:
                    return ShaunEditorFileType.CPP;
                case 3:
                    return ShaunEditorFileType.PYTHON;
                case 4:
                    return ShaunEditorFileType.MARKUP;
                case 5:
                    return ShaunEditorFileType.JSON;
                case 6:
                    return ShaunEditorFileType.SHELL;
                case 7:
                    return ShaunEditorFileType.LATEX;
                case 8:
                    return ShaunEditorFileType.JAVA;

            }

            return ShaunEditorFileType.PLAINTEXT;
        }

        private void setStatus() {
            this.status = "[ ";

            status += this.currentFileType.toString();

            if (this.edited) status += "*";

            status += " ]";

            shedStatus.setText(status);
            shedFilePath.setText(filePath);
        }

        private void setBuffer() {
            this.shedIn.setText(this.fileBuffer);
        }

        void updateUI() {
            // more?
            setStatus();
        }

        void flashMessage(String mesg) {
            shedNotif.setText(mesg);
            shedNotif.setVisibility(View.VISIBLE);
            shedNotif.invalidate();
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {/* Pass */ }
        }

    }

    class AppFetcher extends Thread {
        AppFetcher() { }

        public void run() {
            setupAppMaps();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// INHERITED METHODS ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rootlayout);
        this.hideStatusBar();

        View v = findViewById(R.id.shSTDOUT);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        v.getLayoutParams().height = height / 2;

        SHVARS = new HashMap<>();
        SHVARS.put(Environment.getExternalStorageDirectory().getAbsolutePath(), "$sdcard");
        SHVARS.put(Environment.getRootDirectory().getAbsolutePath(), "$root");

        setupPreferences();
        setupLayout(SceneType.MINIMISED);
        shUnimPrint("(sh-strt) Settings loaded");
        shUnimPrint("(sh-strt) Layout setup");
        //setupAppMaps();
        AppFetcher appFetcher = new AppFetcher();
        appFetcher.start();
        setupScenes();
        setupMan();
        setSubInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.shPrompt = getPrompt();
        this.resetStdin();
        setSubInfo();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (sceneType == SceneType.MAXIMISED) transitTo(SceneType.MINIMISED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        switch (sceneType) {
            case MINIMISED:
                return true;
            case MAXIMISED:
                transitTo(SceneType.MINIMISED);
                return true;
            case EDITOR:
                shedQuitClicked(shEditor.shedIn);
                return true;
        }
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// (RE) INIT ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    void setupPreferences() {
        shPref = getPreferences(MODE_PRIVATE);
        shPrefEditor = shPref.edit();
        defaultiseShPref("uname", "user");
        defaultiseShPref("hname", "droid");
        shPrefEditor.apply();
        shPrefEditor = null;
    }

    void setupLayout(SceneType sc) {
        Typeface inc = Typeface.createFromAsset(getAssets(), "fonts/inconsolata.ttf");
        switch (sc) {
            case MINIMISED:
            case MAXIMISED:
                shSTDOUT = findViewById(R.id.shSTDOUT);
                shSTDIN = findViewById(R.id.shSTDIN);
                shInfo = findViewById(R.id.subinf);
                shClock = findViewById(R.id.shlock);
                shDate = findViewById(R.id.shdate);

                shSTDOUT.setTypeface(inc);
                shSTDIN.setTypeface(inc);
                shInfo.setTypeface(inc);
                shClock.setTypeface(inc);
                shDate.setTypeface(inc);

                shPrompt = getPrompt();
                shSTDOUT.setMovementMethod(new ScrollingMovementMethod());
                shSTDIN.setText(shPrompt);
                shSTDIN.addTextChangedListener(tWatcher);
                shSTDIN.requestFocus();
                break;
            case EDITOR:
                shEditor = new ShaunEditor(inc, editorWatcher);
                break;

        }
    }

    void setupAppMaps() {
        Log.i(TAG, "SETTING UP APP MAPS");
        List<ApplicationInfo> lApps;
        lApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        ignoreapps = new HashMap<>();
        ignoreapps.put("google", "");

        input2type = new HashMap<>();
        input2type.put("ls", 2);
        input2type.put("url", 2);
        input2type.put("help", 2);
        input2type.put("cd", 2);
        input2type.put("clear", 2);
        input2type.put("hide", 2);
        input2type.put("show", 2);
        input2type.put("shset", 2);
        input2type.put("shget", 2);
        input2type.put("apm-list", 2);
        input2type.put("grep", 2);
        input2type.put("man", 2);
        input2type.put("mc", 2);
        input2type.put("shed", 2);
        input2type.put("rm", 2);

        ALIASES = new HashMap<>();
        ALIASES.put("play", "google play store");
        ALIASES.put("amazon", "amazon shopping");
        ALIASES.put("fm", "file manager");
        ALIASES.put("cal", "calendar");
        ALIASES.put("ukp", "url https://www.reddit.com/r/ukpolitics");
        ALIASES.put("cls", "clear");
        ALIASES.put("cam", "blackberry camera");

        input2andrapp = new HashMap<>();
        for (int i = 0; i < lApps.size(); i++) {
            ApplicationInfo ai = lApps.get(i);
            String label = ai.loadLabel(getPackageManager()).toString().toLowerCase();
            if (ignoreapps.get(label) == null && ai.enabled) {
                input2andrapp.put(label, ai.packageName);
                input2type.put(label, 1);
            }
        }
        shUnimPrint("(sh-strt) Applist loaded");
    }

    void setupScenes() {
        Log.i(TAG, "SETTING UP SCENES");
        // init scenes
        rootScene = findViewById(R.id.root);
        hiddenScene = Scene.getSceneForLayout(rootScene, R.layout.hidden, this);
        mainScene = Scene.getSceneForLayout(rootScene, R.layout.main, this);
        editorScene = Scene.getSceneForLayout(rootScene, R.layout.sheditor, this);
    }

    void setupMan() {
        manmap = new HashMap<>();

        manmap.put("apm-list", "Android Package Manager: List\nLists all installed apps.\n\nUsage:\n  apm-list [-ld]\n\nOptions:\n  -l  lowercase list (useful for grepping results)\n  -d  detailed list");
        manmap.put("cd", "Change directory.\n\nUsage:\n  cd <relative directory>");
        manmap.put("ls", "List contents of current directory, or selected directory.\n\nUsage:\n  ls [<dir>]");
        manmap.put("hide", "Hides UI. UI is unhidden on output to STDOUT.");
        manmap.put("show", "Shows UI. UI is also shown on output to STDOUT.");
        manmap.put("shaun", "SHAUN Launcher\nMinimal Android launcher, intended for the power user and the terminal geek." +
                        "\n\nSHAUN is distributed under the GPLv3 License. You can view the full license by executing the command \"license\" at the home shell.");
        manmap.put("clear", "Clear all output from buffer history.");
        manmap.put("shset", "SHAUN Settings manager\nSets a value in the settings database.\n\nUsage:\n  shset <key> <value>");
        manmap.put("shget", "SHAUN Settings manager\nGets a value in the settings database.\nLaunch with no options to get all stored values.\n\nUsage:\n  shget [<key>]");
        manmap.put("man", "Manual\nGiven the name of an app, returns its function and usage.\n\nUsage:\n  man <appname>");
    }

    void defaultiseShPref(String key, String defval){
        String res = shPref.getString(key, "unset");
        if (res.equals("unset")) {
            if (shPrefEditor != null) {
                // editor already made: bulk changes
                shPrefEditor.putString(key, defval);
            }
            else {
                // no editor: remake here
                shPrefEditor = shPref.edit();
                shPrefEditor.putString(key, defval);
                shPrefEditor.apply();
            }
        }
    }

    String[] trimArr(String[] toTrim) {
        if (toTrim == null) return null;
        ArrayList<String> tt = new ArrayList<>(Arrays.asList(toTrim));

        for (int i = 0; i < toTrim.length; i++){
            int j = toTrim.length - i - 1;
            if (toTrim[j] == null || toTrim[j].equals("")){
                tt.remove(j);
            }
        }

        return tt.toArray(new String[tt.size()]);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// UI & CONTROL /////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    boolean checkNGetWritePermission() {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG,"Permission is granted");
            return true;
        } else {

            Log.v(TAG,"Permission is revoked");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        return (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    void inputTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() < 1) {
            return;
        }
        else if (s.length() < shPrompt.length()){
            setStdin(shPrompt + prevCommand);
            return;
        }
        else if (count < before){
            return;
        }
        char c = s.charAt(start);
        if (c == '\n') {
            String ss = s.toString().replaceAll("\n", "");
            setStdin(ss);

            // do something with input
            String entered = ss.substring(shPrompt.length());
            prevCommand = entered;
            int rv = shTabComplete(entered);
            if (rv != 1) shUnimPrint("\n" + ss);
            switch (rv) {
                case 0: // no match
                    shPrint(entered + ": command not found");
                    resetStdin();
                    break;
                case 1: // partial match
                    break;
                case 2: // total match
                    if (!entered.contains(" ")) {
                        shExec(entered, null);
                    }
                    else {
                        String command = entered.substring(0, entered.indexOf(" "));
                        String[] args = entered.substring(entered.indexOf(" ")).split(" ");
                        shExec(command, args);
                    }
                    break;
                case 3:
                    shExec(entered, null);
                    break;
            }
        }
    }

    int shTabComplete(String entered) {
        // takes "entered" text and compares to shApps
        // gets most complete app name
        // returns an int indicating how the completion went:
        //      0: did not match anything
        //      1: partially complete
        //      3: complete + unique

        String command;
        if (entered.contains(" ")){
            command = entered.substring(0, entered.indexOf(" "));
        }
        else {
            command = entered;
        }

        String bestMatchName;
        int bestMatchLength;

        String aliasres = ALIASES.get(command);
        if (aliasres != null) {
            setStdin(shPrompt + aliasres);
            return 1;
        }

        Object res = input2type.get(entered.toLowerCase());
        if (res != null) {
            // android app or built in app, no args, command contains " "
            return 3;
        }

        res = input2type.get(command.toLowerCase());
        if (res != null) {
            // android app or built in app, with args, command contains no " "
            return 2;
        }


        Tuple<String, Integer> match = getBestMatch(entered, input2andrapp.keySet());
        // do other autocompletion stuff

        bestMatchName = match.x;
        bestMatchLength = match.y;

        if (bestMatchLength > 0) {
            setStdin(shPrompt + bestMatchName);
            return 1;
        }

        return 0;
    }

    Tuple<String, Integer> getBestMatch(String toCheck, Set<String> against){
        int bestMatchLength = 0;
        String bestMatchName = "";

        if (against.contains(toCheck)) return new Tuple<>(toCheck, -1);

        for (String k: against) {
            String sub_check;
            for (int j = 0; j < toCheck.length(); j++) {
                sub_check = toCheck.substring(0, toCheck.length() - j).toLowerCase();
                if (k.toLowerCase().startsWith(sub_check)) {
                    if (toCheck.length() - j > bestMatchLength) {
                        bestMatchName = k;
                        bestMatchLength = toCheck.length() - j;
                        Log.d(TAG, "shTabComplete: potential match: " + k);
                        break;
                    }
                }
            }
        }

        if (bestMatchLength > 0) {
            return new Tuple<>(bestMatchName, bestMatchLength);
        }
        else {
            return new Tuple<>(toCheck, 0);
        }
    }

    void setSubInfo(){
        PackageInfo pInfo;
        String version = "vXX";
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        }
        catch (Exception e) {
            // pass
        }
        String text = "";
        text += "shaun v" + version + "\n";
        text += "android: " + Build.VERSION.RELEASE + "\n";
        String[] dirs = shCurrentDirectory.split("/");
        if (SHVARS.get(shCurrentDirectory) != null) {
            text += "[" + SHVARS.get(shCurrentDirectory) + "]";
        }
        else if (dirs.length > 2) {
            text += "[.../" + dirs[dirs.length - 1] + "]";
        }
        else {
            text += "[" + shCurrentDirectory + "]";
        }
        shInfo.setText(text);
    }

    void hideStatusBar(){
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    void refreshUI(){
        shPrompt = getPrompt();
        setStdin(shPrompt);
        setSubInfo();
    }

    public void clockClicked(View v) {
        shExec("clock", null);
    }

    public void calClicked(View v) {
        shExec("calendar", null);
    }

    public void shedSaveClicked(View v) {
        if (!shEditor.filePath.equals("")) {
            shEditor.writeBufferToFile();
        }
        else {
            getInput();
        }
    }

    public void shedQuitClicked(View v) {
        if (shEditor.canQuit()) {
            transitTo(SceneType.MINIMISED);
        }
        else {
            shEditor.flashMessage("Can't quit right now.");
        }
    }

    public void shedNotifClicked(View v) {
        shEditor.shedNotif.setVisibility(View.INVISIBLE);
    }

    void setHidden(boolean state) {

        plcBuff = shSTDOUT.buffer;

        if (state) {
            transitTo(SceneType.MINIMISED);
        }
        else {
            transitTo(SceneType.MAXIMISED);
        }

        setSubInfo();

        shSTDOUT.buffer = plcBuff;
        shSTDOUT.refreshFromBuffer();

    }

    boolean isHidden() {
        return (sceneType == SceneType.MINIMISED);
    }

    void transitTo(SceneType sc) {
        switch (sc) {
            case MINIMISED:
                TransitionManager.go(hiddenScene, transistion);
                break;
            case MAXIMISED:
                TransitionManager.go(mainScene, transistion);
                break;
            case EDITOR:
                TransitionManager.go(editorScene, transistion);
                break;
        }
        setupLayout(sc);
    }

    void getInput(){
        shEditor.filePath = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New file name:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                shEditor.filePath = shCurrentDirectory + "/" + input.getText().toString();
                shEditor.writeBufferToFile();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// SHELL STUFF //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public String getPrompt(){
        String shprompt = shPref.getString("uname", getResources().getString(R.string.uname)) + "@" + shPref.getString("hname", getResources().getString(R.string.hname)) + "> ";
        shSTDIN.minSel = shprompt.length();
        return shprompt;
    }

    public void resetStdin(){
        this.shSTDIN.setText(shPrompt, TextView.BufferType.EDITABLE);
        this.shSTDIN.setSelection(shPrompt.length());
    }

    public void setStdin(String ss) {
        this.shSTDIN.setText(ss, TextView.BufferType.EDITABLE);
        this.shSTDIN.setSelection(ss.length());
    }

    void shPrint(String toPrint, String end) { shPrint(toPrint, end, true); }

    void shPrint(String toPrint) { shPrint(toPrint, "\n", true); }

    void shUnimPrint(String toPrint) { shPrint(toPrint, "\n", false); }

    void shPrint(String toPrint, String end, boolean isImportant) {
        toPrint += end;
        if (redirect) {
            reBuff.add(toPrint);
        }
        else {
            if (isHidden() && isImportant){
                setHidden(false);
                Thread.yield();
            }
            shSTDOUT.addToBuffer(toPrint);
        }
    }

    public void shExec(String command, String[] args) {
        Log.d(TAG, "shExec: " + command);
        resetStdin();

        if (args != null) args = trimArr(args);

        if (input2type.get(command) == 1) {
            Intent li = getPackageManager().getLaunchIntentForPackage(input2andrapp.get(command));
            startActivity(li);
        }
        else {

            try {
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i].equals("|")) {
                            if (args[i + 1].equals("grep")) {
                                String nc = "grep";
                                String[] na = new String[args.length - 1];
                                na[0] = args[i + 2]; // pattern
                                na[1] = command; // command
                                System.arraycopy(args, 0, na, 2, args.length - 3);

                                command = nc;
                                args = na;

                                Log.d(TAG, "shExec: NC: " + nc);
                                for (String s : na) {
                                    Log.d(TAG, "shExec:  ARGS:  " + s);
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {/*Pass*/}

            switch (command) {
                case "ls":
                    shb_ls(args);
                    break;
                case "url":
                    shb_url(args);
                    break;
                case "help":
                    shb_man(args);
                    break;
                case "man":
                    shb_man(args);
                    break;
                case "clear":
                    shb_clear();
                    break;
                case "hide":
                    setHidden(true);
                    break;
                case "show":
                    setHidden(false);
                    break;
                case "cd":
                    shb_cd(args);
                    break;
                case "shset":
                    shb_shset(args);
                    break;
                case "shget":
                    shb_shget(args);
                    break;
                case "grep":
                    shb_grep(args);
                    break;
                case "apm-list":
                    shb_apm_list(args);
                    break;
                case "mc":
                    shb_mc(args);
                    break;
                case "shed":
                    shb_shed(args);
                    break;
                case "rm":
                    shb_rm(args);
                    break;
                default:
                    shPrint("Command " + command + " not found.");
                    break;

            }
        }
    }

    void shb_url(String[] args) {
        boolean fine = true;

        if (args == null) { fine = false; }
        else if (args.length < 1){ fine = false; }

        if (!fine) {
            shPrint("Incorrect syntax. \n\nUsage:\n  url <address>");
            return;
        }

        if (!args[0].startsWith("https://") && !args[0].startsWith("http://")){
            args[0] = "http://" + args[0];
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(args[0]));
        startActivity(i);
    }

    void shb_ls(String[] args) {

        if (args == null) args = new String[0];

        File directory = null;
        boolean listify = false, allificate = false;

        for (String s: args) {
            Log.d(TAG, "shb_ls: " + s);
            if (!s.startsWith("-")) {
                try {
                    directory = new File(s);
                    Log.d(TAG, "shb_ls: SUCCESSFULLY OPENED FILE OF " + s);
                } catch (Exception e) {
                    shPrint("Unrecognised directory: \"" + s + "\"");
                }
            }
            else {
                for (char c : s.toCharArray()) {
                    switch (c) {
                        case 'a':
                            allificate = true;
                        case 'l':
                            listify = true;
                            break;
                        case '-':
                            break;
                        default:
                            shPrint("Unrecognised option. \n\nUsage:\n  ls [-la]");
                            break;
                    }
                }
            }
        }

        if (directory == null){
            directory = new File(shCurrentDirectory);
        }

        File[] files = directory.listFiles();

        if (files == null) files = new File[0];

        if (files.length > 0) {
            for (File f : files) {
                String prefix = "", suffix = "";

                if (f.isDirectory()) {
                    prefix = "[";
                    suffix = "]";
                }
                else if (f.canExecute()) {
                    suffix += "*";
                }

                String end = " ";
                if (listify) end = "\n";
                if (allificate) prefix = (new SimpleDateFormat("dd-MM-yyyy", Locale.UK)).format(f.lastModified()) + " " + prefix;
                if (allificate || !f.isHidden()) {
                    shPrint(prefix + f.getName() + suffix, end);
                }
            }
        }
        else {
            shPrint("\""+ directory.getPath()+ "\" is empty directory.");
        }
    }

    void shb_cd(String[] args) {
        // change directory
        if (args.length < 1){
            // Changing to current directory... ?
            return;
        }

        if (args.length > 1) {
            shPrint("Syntax error.\n\nUsage:\n  cd <dir>");
            return;
        }


        String newpath;

        if (args[0].startsWith("$SD") || args[0].startsWith("£SD")){
            newpath = Environment.getExternalStorageDirectory().getAbsolutePath() + args[0].replace("$SD", "");
        }else if (args[0].contains("$SD") || args[0].contains("£SD")){
            shPrint("Error. Absolute var must be used at begining of path.");
            return;
        }else {
            newpath = shCurrentDirectory + "/" + args[0];
        }


        Log.d(TAG, "shb_cd: CHANGING DIR TO " + newpath);
        String[] npspl = newpath.split("/");
        StringBuilder sb = new StringBuilder();
        sb.append(npspl[0]);
        for (int i = 1; i < npspl.length - 1; i++){
            if (!npspl[i + 1].equals("..")){
                sb.append("/");
                sb.append(npspl[i]);
            }
        }
        newpath = sb.toString();
        if (!npspl[npspl.length - 1].equals("..")){newpath += "/" + npspl[npspl.length - 1]; }
        if (newpath.equals("")){
            shPrint("Lack permissions to access root directory.");
            return;
        }
        File newdirectory = new File(newpath);

        if (newdirectory.isDirectory()) {
            shCurrentDirectory = newdirectory.getAbsolutePath();
            setSubInfo();
        }
        else {
            shPrint(args[0] + " is not a directory.");
        }
    }

    void shb_clear() {
        shSTDOUT.buffer.clear();
        shSTDOUT.refreshFromBuffer();
    }

    void shb_shset(String[] args) {

        boolean fine = true;

        if (args == null){fine = false;}
        else if (args.length == 0){fine = false;}
        else if (args.length == 1 && !args[0].equals("help")){fine = false;}
        else if (args.length > 2){fine = false;}

        if (!fine){
            shPrint("Syntax error. Correct syntax is:");
            shPrint("  shset <key> <value>");
            shPrint("  shset help");
        }
        else if (args.length == 1 && args[0].equals("help")) {
            shPrint("shset: shaun launcher settings manager.");
            shPrint("\nUsage:");
            shPrint("  shset <key> <value>");
            shPrint("  shset help");
        }
        else {
            shPrint("Set:");
            shPrint("  " + args[0] + " : " + args[1]);
            shPrefEditor.putString(args[0], args[1]);
            shPrefEditor.commit();
            refreshUI();
        }
    }

    void shb_shget(String[] args) {

        boolean fine = true;
        if (args != null && args.length > 1){fine = false;}

        if (!fine){
            shPrint("Syntax error. Correct syntax is:");
            shPrint("  shget [<key>]");
            shPrint("  shget help");
        }
        else {
            shPrint("Get:");
            if (args == null || args.length == 0)  {
                Map<String, ?> mk = shPref.getAll();

                for (String k: mk.keySet()) {
                    shPrint("  " + k + " : " + shPref.getString(k, "Oops! Something went wrong"));
                }
            }
            else {
                shPrint("  " + args[0] + " : " + shPref.getString(args[0], "NOT_FOUND"));
            }
        }
    }

    void shb_grep(String[] args) {

        // grep <pattern> <command> <*args>

        String[] sArgs;

        if (args.length > 2) {
            sArgs = new String[args.length - 2];
            System.arraycopy(args, 2, sArgs, 0, args.length - 2);
        }
        else {
            sArgs = null;
        }

        redirect = true;
        shExec(args[1], sArgs);
        redirect = false;

        ArrayList<String> oBuff = new ArrayList<>(0);

        for (String s: reBuff) {
            if (Pattern.compile(args[0]).matcher(s).find()) {
                oBuff.add(s);
            }
        }

        reBuff.clear();

        for (String s: oBuff) {
            shPrint(s);
        }
    }

    void shb_apm_list(String[] args) {
        List<ApplicationInfo> lApps;
        lApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        boolean lowercasify = false, detailed = false;

        if (args != null){

            for (String a: args){
                if (a.contains("l")) {
                    lowercasify = true;
                }

                if (a.contains("d")) {
                    detailed = true;
                }
            }
        }

        shPrint("Installed packages:");
        ArrayList<String> appNameList = new ArrayList<>(0);

        for (ApplicationInfo ai: lApps) {
            String appName = ai.loadLabel(getPackageManager()).toString();
            if (lowercasify) appName = appName.toLowerCase();

            if (detailed){
                String enabledIndicator = "", ignoredIndicator = "";
                if (!ai.enabled) enabledIndicator = " [DISABLED]";
                if (ignoreapps.get(appName) != null) ignoredIndicator = " [IGNORED]";
                appName = appName + enabledIndicator + ignoredIndicator;
            }
            appNameList.add(appName);
        }

        Collections.sort(appNameList, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        for (String s: appNameList) {
            shPrint(s);
        }
    }

    void shb_man(String[] args) {

        if (args != null && args.length != 0) {
            // args is app name
            String res = manmap.get(args[0]);
            if (res != null) {
                shPrint(res);
            }
            else {
                shPrint(" Manual error: " + args[0] + " not found!");
            }
        }
        else {
            // no args
            shPrint(manmap.get("shaun"));

            shPrint("Manual entries index:");
            for (String k: manmap.keySet()) {
                shPrint("  - " + k);
            }
        }
    }

    void shb_mc(String[] args) {
        // With thanks to the answerer here:
        // https://stackoverflow.com/questions/27307265/control-playback-of-the-spotify-app-from-another-android-app
        int keycode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
        if (args != null && args.length > 0) {
            switch (args[0]){
                case "next":
                    keycode = KeyEvent.KEYCODE_MEDIA_NEXT;
                    break;
                case "prev":
                    keycode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                    break;
                default:
                    shPrint("Option not understood: " + args[0] + "\nAvailable options are:\n  next\n  prev\nor with no options to toggle playing.");
                    return;
            }
        }

        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.setPackage("com.spotify.music");
        synchronized (this) {
            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keycode));
            sendOrderedBroadcast(i, null);

            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, keycode));
            sendOrderedBroadcast(i, null);
        }
    }

    void shb_shed(String[] args) {
        transitTo(SceneType.EDITOR);

        if (args == null || args.length == 0) {
            shEditor.shedFilePath.setText(R.string.newFileString);
        }
        else {
            switch (shEditor.openFileToBuffer(shCurrentDirectory + "/" + args[0])) {
                case -1:
                    // could not open file
                case 1:
                    // opening new file
                    // anything TODO here?
                case 0:
                default:
                    break;
            }
        }
    }

    void shb_rm(String[] args) {
        if (args == null || args.length != 1) {
            shPrint("Remove what? Select a file to delete.");
            return;
        }

        File f = new File(shCurrentDirectory + "/" + args[0]);

        if (!f.exists()) {
            shPrint("File is not existant.");
            return;
        }

        if (f.isDirectory()) {
            shPrint("Cannot delete file: is directory.");
            return;
        }

        f.delete();

        if (f.exists()) {
            shPrint("File was not deleted!");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// INTERPRETER //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    Map<String, String> getGlobalVariables() {
        Map<String, String> rv = new HashMap<>(0);
        rv.put("", "");
        return rv;
    }


    void interpret(String[] script) {

        script = trimArr(script);

        Map<String, String> script_memory = getGlobalVariables();

        //TODO
    }*/

    /*
     TODO:

      - extend reach of settings apps:
          - theming
          - fontsize
          - stuff like that
      - Find better font for clock...
      - Proper implementation of the pipe operator
      - Built in text editor
        - flashMessage() should run asyncronously
      - (Ruby? PYTHON?) Extension interpreter
      - Change input computation to a secondary thread (constantly listening
        if a command is sent, executes when a command is detected -- keeps
        execution off the main/UI thread).

      KNOWN BUGS:

      - When launching "PIXEL LAUNCHER" from SHAUN, bugs out

          - "java.lang.NullPointerException: Attempt to invoke virtual method
            'boolean android.content.Intent.migrateExtraStreamToClipData()'
            on a null object reference"

          - Just seems to be pixel launcher, not noticed it with other apps
            (maybe its just for homescreen apps?)

      - When deleting a subsequent file, many error messages show but the
        deletion goes on without incident.

          - No idea what is causing this. It seems to be an input error?

          - There should be no way of the code repeating. Perhaps something
            to do with the way the command is communicated to the program?
    */


}
