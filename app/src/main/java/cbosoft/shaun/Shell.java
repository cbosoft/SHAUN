package cbosoft.shaun;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.widget.TextView;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;

import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class Shell {

    public String WorkingDirectory = "";

    // "Environment vars"
    ShellApp[] Apps;

    // android stuff
    TextView _stdout;
    EditText _stdin;
    int _maxLines = 22;
    List<ApplicationInfo> _appinfolist;
    Activity _pa;
    
    public Shell (TextView stdout, EditText stdin, List<ApplicationInfo> lAppInfo, Activity pa) {

        // Create watcher for checking when <RET> or <TAB> pressed


        //generateShApps();
    }



    



    public void Print(String toPrint) {
	    // Outputs a line to the shell.
	    // If shell is full, cuts off oldest line and adds new line,
	    // otherwise just adds new line to shell.
	    // TODO

        String cur = this._stdout.getText().toString(), outp = "";
        String[] cur_lines = cur.split("\n");
        int offs = cur_lines.length - this._maxLines;
        if (offs < 0) {offs = 0; }
        for (int i = offs; i < cur_lines.length; i++) {
            outp += cur_lines[i] + "\n";
        }
        outp += toPrint;
    }

    public String Readline() {
	    // Gets a line in from the user
	    // Not sure how feasible this is, with the terminal set up as it is
	    // via a textview
	    // TODO

        return "";
    }

    public int Exec(String input) {
        // Given a line in from the user, parses, searches $(PATH) for
        // a  likely command, executes if complete, or completes (as
        // far as is possible) if not.
        // i.e., implements a simple form of "tab" completion, only
        // using enter instead of tab.
        // TODO

        // Tries to find a ShellApp that matches the input, split
        // by ' ' (space) characters.

	    return 0;
    }
}


