package uk.co.octatec.scdds.utilities;

import java.io.Serializable;

/**
 * Created by Jeromy Drake on 06/05/16
 */
public class SimpleData implements Serializable {
    public String data1;
    public int data2;

    public SimpleData(String data1, int data2) {
        this.data1 = data1;
        this.data2 = data2;
    }

    @Override
    public String toString() {
        return "SimpleData{" +
                "data1='" + data1 + '\'' +
                ", data2=" + data2 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleData that = (SimpleData) o;

        if (data2 != that.data2) return false;
        return data1 != null ? data1.equals(that.data1) : that.data1 == null;

    }

    @Override
    public int hashCode() {
        int result = data1 != null ? data1.hashCode() : 0;
        result = 31 * result + data2;
        return result;
    }
}
