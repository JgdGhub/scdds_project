package uk.co.octatec.scdds.utilities;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Jeromy Drake on 09/05/2016.
 */
public class DataWithDate implements Serializable {

    long serialVersionUID = 1;

    int id;
    Date date = new Date();

    public DataWithDate(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataWithDate data = (DataWithDate) o;

        if (id != data.id) return false;
        return date != null ? date.equals(data.date) : data.date == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }
}
