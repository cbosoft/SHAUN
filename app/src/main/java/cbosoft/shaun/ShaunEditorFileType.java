package cbosoft.shaun;

public enum ShaunEditorFileType {
    PLAINTEXT,
    C,
    CPP,
    PYTHON,
    MARKUP,
    JSON,
    SHELL,
    LATEX,
    JAVA;

    @Override
    public String toString() {
        return this.name();
    }
}
