package uk.co.octatec.scdds_samples.custom_serializer;

import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;
import uk.co.octatec.scdds_samples.basic_example.Data;

/**
 * Created by Jeromy Drake on 07/06/2016.
 */
public class DataSerializerFactory implements SerializerFactory<String, Data> {
    @Override
    public Serializer<String, Data> create() {
        return new DataSerializer();
    }
}
