package cbosoft.shaun;

import android.content.Intent;
import android.content.pm.ApplicationInfo;

import java.util.Map;

public class ShellApp {
    // Basic executable for the shell.
    // encompasses built ins, and android system installed apps.
    // One is run by the shell, the other is launched and managed as
    // a new process by the package manager.

    public String Name = "";

    public Home parent;

    public ApplicationInfo _ai;

    public ShellApp (String name) {
        // Constructor
        this.Name = name;
    }

    public ShellApp (String name, ApplicationInfo ai){
        this.Name = name;
        this._ai = ai;
    }

    public void execute(String[] args, Map<String, String> kwargs) {
        // This method is called when the program is run.
        // To be as general as possible for this prototype,
        // a list of String args may be passed, as well as
        // key-value pair mapped kwargs (like a Python method)
        // This can be overridden for apps which aren't just
        // representations of android apps.
        Intent li = parent.getPackageManager().getLaunchIntentForPackage(this._ai.packageName);
        parent.startActivity(li);
    }
}
