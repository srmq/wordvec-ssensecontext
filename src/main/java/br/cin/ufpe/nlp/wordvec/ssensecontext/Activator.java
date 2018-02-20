package br.cin.ufpe.nlp.wordvec.ssensecontext;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.cin.ufpe.nlp.api.bagofwords.TfIdfComputerService;
import br.cin.ufpe.nlp.api.tokenization.Tokenizer;
import br.cin.ufpe.nlp.api.tokenization.TokenizerFactory;
import br.cin.ufpe.nlp.api.vectors.VectorVocabService;




public class Activator implements BundleActivator {
	private String wordsPath;
	private String ssenseOutPath;
	private String ssensesInputPath;
	private File wordVectorFile;
	private String indexDir;
	private static Logger logger = LoggerFactory.getLogger(Activator.class);
	boolean firstTime = true;
	private Map<String, Object> services = new HashMap<String, Object>(2);

	
	private class ServiceAvailableListener<T> implements ServiceListener {

		private BundleContext context;
		
		public ServiceAvailableListener(BundleContext context) {
			this.context = context;
		}
		
		public void serviceChanged(ServiceEvent ev) {
			if (ev.getType() != ServiceEvent.UNREGISTERING && firstTime) {
				ServiceReference servRef = ev.getServiceReference();
				@SuppressWarnings("unchecked")
				T service = (T) context.getService(servRef);
				String[] objectClass = (String[]) servRef.getProperty(Constants.OBJECTCLASS);
				services.put(objectClass[0], service);
				logger.info(service.getClass().getName() + " became available, continuing");
				if(isReady()) {
					try {
						doStuff();
					} catch (IOException e) {
						throw new IllegalStateException("IOException when trying doStuff following serviceChanged", e);
					}
					firstTime = false;
				}
			}
			
		}
	}
	

	@SuppressWarnings("rawtypes")
	public void start(BundleContext context) throws Exception {
		wordsPath = System.getenv("TEXT_WORDSPATH");
		if (wordsPath == null) {
			throw new IllegalArgumentException("Missing environment variable TEXT_WORDSPATH");
		}
		ssensesInputPath = System.getenv("TEXT_SSENSESPATH");
		if (ssensesInputPath == null) {
			throw new IllegalArgumentException("Missing environment variable TEXT_SSENSESPATH");
		}
		
		ssenseOutPath = System.getenv("SSENSECONTEXT_OUTPUTPATH");
		if (ssenseOutPath == null) {
			throw new IllegalArgumentException("Missing environment variable SSENSECONTEXT_OUTPUTPATH");
		}
		
		String vectorFile = System.getenv("VECTOR_FILE");
		if (vectorFile == null) {
			throw new IllegalArgumentException("Missing environment variable VECTOR_FILE");
		}
		
		wordVectorFile = new File(vectorFile);
		
		indexDir = context.getProperty("java.io.tmpdir") + File.separator + wordVectorFile.getName() + ".luceneIndex";
		
		ServiceReference refTfIdfService = context.getServiceReference(TfIdfComputerService.class.getName());
		if (refTfIdfService == null) {
			logger.warn("TfIdfComputerService not found, waiting for it");
			context.addServiceListener(new ServiceAvailableListener<TfIdfComputerService>(context), "(" + Constants.OBJECTCLASS + "=" + TfIdfComputerService.class.getName() + ")");			
		} else {
			logger.info("TfIdfComputerService available, continuing");
			TfIdfComputerService tfIdfServ = (TfIdfComputerService) context.getService(refTfIdfService);
			services.put(TfIdfComputerService.class.getName(), tfIdfServ);
		}
		
		ServiceReference vectorVocabServiceRef = context.getServiceReference(VectorVocabService.class.getName());
		if (vectorVocabServiceRef == null) {
			logger.warn("VectorVocabService service not found, waiting for it");
			context.addServiceListener(new ServiceAvailableListener<VectorVocabService>(context), "(" + Constants.OBJECTCLASS + "=" + VectorVocabService.class.getName() + ")");
		} else {
			logger.info("VectorVocabService available, continuing");
			VectorVocabService vecVocabServ = (VectorVocabService) context.getService(vectorVocabServiceRef);
			services.put(VectorVocabService.class.getName(), vecVocabServ);
		}
		
		ServiceReference[] tokenservices = context.getServiceReferences(TokenizerFactory.class.getName(), "(type=line)");
		if (tokenservices == null) {
			logger.warn("LineTokenizer not found, waiting for it...");
			context.addServiceListener(new ServiceAvailableListener<TokenizerFactory>(context), "(&(" + Constants.OBJECTCLASS + "=" + TokenizerFactory.class.getName() + ")(type=line))");			
		} else {
			@SuppressWarnings("unchecked")
			TokenizerFactory<Tokenizer<String>> tokenizerFactory = (TokenizerFactory<Tokenizer<String>>) context.getService(tokenservices[0]);
			services.put(TokenizerFactory.class.getName(), tokenizerFactory);
			if (isReady()) {
				doStuff();
				firstTime = false;
			}
		}
	}
	
	private void doStuff() throws IOException {
		VectorVocabService vecVocabService = (VectorVocabService) services.get(VectorVocabService.class.getName());
		@SuppressWarnings("unchecked")
		TokenizerFactory<Tokenizer<String>> tokenizer = (TokenizerFactory<Tokenizer<String>>) services.get(TokenizerFactory.class.getName());
		TfIdfComputerService tfIdfService = (TfIdfComputerService) services.get(TfIdfComputerService.class.getName());
		SSenseContextController controller = new SSenseContextController(this.wordsPath, this.ssensesInputPath, this.ssenseOutPath, vecVocabService, tokenizer, this.indexDir, this.wordVectorFile, tfIdfService); 
		controller.process();
	}
	
	
	private boolean isReady() {
		return services.containsKey(VectorVocabService.class.getName()) && services.containsKey(TokenizerFactory.class.getName()) && services.containsKey(TfIdfComputerService.class.getName());
	}


	public void stop(BundleContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}


}
