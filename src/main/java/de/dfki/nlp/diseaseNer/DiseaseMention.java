package de.dfki.nlp.diseaseNer;

/**
 * Created by philippe on 1/31/17.
 */
public class DiseaseMention {

    private int start;
    private int end;
    private String text;

    public DiseaseMention(int start, int end, String text) {
        this.start = start;
        this.end = end;
        this.text = text;
    }

    @Override
    public String toString() {
        return "de.dfki.nlp.diseaseNer.DiseaseMention{" +
                "start=" + start +
                ", end=" + end +
                ", text='" + text + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiseaseMention that = (DiseaseMention) o;

        if (start != that.start) return false;
        if (end != that.end) return false;
        return text != null ? text.equals(that.text) : that.text == null;
    }

    @Override
    public int hashCode() {
        int result = start;
        result = 31 * result + end;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
