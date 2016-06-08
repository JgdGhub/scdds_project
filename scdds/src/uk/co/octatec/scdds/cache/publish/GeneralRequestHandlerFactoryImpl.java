package uk.co.octatec.scdds.cache.publish;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class GeneralRequestHandlerFactoryImpl implements GeneralRequestHandlerFactory {

    static final GeneralRequestHandler instance = new GeneralRequestHandlerImpl();

    @Override
    public GeneralRequestHandler getInstance() {
        return instance;
    }
}
