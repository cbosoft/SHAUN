package cbosoft.shaun;

import android.util.Log;

import java.util.Arrays;
import java.util.Map;

import me.xdrop.fuzzywuzzy.FuzzySearch;

import static android.support.constraint.Constraints.TAG;

public class InputCommand {

    String appName;

    String appType;
    String inputRegex;
    String packageName;
    String userInput;

    Boolean needsArgs = Boolean.FALSE;


    public InputCommand(String appName, String regex, String appType, Boolean needsArgs) {
        this.appName = appName;
        this.inputRegex = regex;
        this.appType = appType;
        this.needsArgs = needsArgs;
    }

    public InputCommand(String appName, String regex, String appType, String packageName) {
        this.appName = appName;
        this.inputRegex = regex;
        this.appType = appType;
        this.packageName = packageName;
    }

    public int matches(String input) {
        this.userInput = input;
        if (input.matches(inputRegex)) {
            Log.d(TAG, "matches: " + input);
            return 100;
        }

        //if (this.appType.equals("alias")) return 0;

        int res = FuzzySearch.ratio(appName, input);

        if (appName.startsWith(input)) res += 50;
        if (this.needsArgs && appName.startsWith(input)) res += 50;

        if (res < 50) return 0;
        return res;
    }

    public String[] getLaunchArgs(){
        String[] sp = this.userInput.split(" ");
        return Arrays.copyOfRange(sp, 1, sp.length);
    }
}
