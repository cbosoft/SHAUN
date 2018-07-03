package cbosoft.shaun;

import android.graphics.Color;
import android.util.Log;

import java.util.Arrays;

import me.xdrop.fuzzywuzzy.FuzzySearch;

import static android.support.constraint.Constraints.TAG;

public class InputCommand {

    String appName;

    AppType appType;
    String packageName;
    String userInput;

    private String inputRegex;
    private String usageString;

    private Boolean needsArgs = Boolean.FALSE;


    // builtins
    InputCommand(String appName, String regex, String usageString, Boolean needsArgs) {
        this.appName = appName;
        this.inputRegex = regex;
        this.appType = AppType.BUILTIN;
        this.usageString = usageString;
        this.needsArgs = needsArgs;
    }

    // android
    InputCommand(String appName, String regex, String packageName) {
        this.appName = appName;
        this.inputRegex = regex;
        this.appType = AppType.ANDROID;
        this.packageName = packageName;
    }

    // alias
    InputCommand(String appName, String regex) {
        this.appName = appName;
        this.inputRegex = regex;
        this.appType = AppType.ALIAS;
    }

    public int matches(String input) {
        this.userInput = input;
        if (this.appType == AppType.ALIAS && input.equals(this.appName)) return 0; // don't match alias if input is exact
        if (input.matches(inputRegex)) {
            Log.d(TAG, "matches: " + input);
            return 100;
        }

        int res = FuzzySearch.ratio(appName, input);

        if (appName.startsWith(input)) res += 50; // prefer apps which match input
        if (this.needsArgs && appName.startsWith(input.split(" ")[0])) res += 50; // to offset the lack of fuzzy matching otherwise
        if (this.appType == AppType.ALIAS) res += 25; // prefer alias, if not already matching
        if (res < 50) return 0;

        return res;
    }

    public String getDisplayString(int usageCount) {
        String name = this.appName, info = " ";

        String nmColor = "#AAAAAF";
        String ucColor = "#666666";
        String ifColor = "#3388CC";

        String ucString;

        switch (this.appType) {
            case ALIAS:
                name =  this.inputRegex;
                info += "[ALIAS->" + this.appName + "]";
                break;
            case BUILTIN:
                name = this.usageString;
                info += "[SH]";
                break;
            case ANDROID:
                info += "[APK]";
                break;
        }


        if (usageCount > 9) {
            nmColor = "#DFDFDF";
            name = "<b>" + name + "</b>";
            ucString = "+";
        }
        else {
            ucString = Integer.toString(usageCount);
        }


        return "<font color=" + nmColor + ">" + name + "</font> <small><font color=" + ucColor + ">(" + ucString + ")</font>" + "<font color=" + ifColor + ">" + info + "</font></small>";
    }

    public String[] getLaunchArgs(){
        String[] sp = this.userInput.split(" ");
        return Arrays.copyOfRange(sp, 1, sp.length);
    }
}
