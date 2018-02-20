package br.cin.ufpe.nlp.wordvec.ssensecontext;

import java.io.File;
import java.io.IOException;

import br.cin.ufpe.nlp.api.bagofwords.TfIdfComputerService;
import br.cin.ufpe.nlp.api.bagofwords.TfIdfInfo;
import br.cin.ufpe.nlp.api.tokenization.Tokenizer;
import br.cin.ufpe.nlp.api.tokenization.TokenizerFactory;
import br.cin.ufpe.nlp.api.transform.DocumentProcessorNToOne;
import br.cin.ufpe.nlp.api.vectors.VectorVocab;
import br.cin.ufpe.nlp.api.vectors.VectorVocabService;
import br.cin.ufpe.nlp.util.RecursiveTransformer;

public class SSenseContextController {
	
	private String wordsPath;
	private String ssensesInputPath;
	private String ssenseOutPath;
	private VectorVocabService vecVocabService;
	private TokenizerFactory<Tokenizer<String>> tokenizer;
	private String indexDir;
	private File wordVectorfile;
	private TfIdfComputerService tfIdfService;

	public SSenseContextController(String wordsPath, String ssensesInputPath, String ssenseOutPath, VectorVocabService vecVocabService, TokenizerFactory<Tokenizer<String>> tokenizer,
			String indexDir, File wordVectorFile, TfIdfComputerService tfIdfService) {
		this.wordsPath = wordsPath;
		this.ssensesInputPath = ssensesInputPath;
		this.ssenseOutPath = ssenseOutPath;
		this.vecVocabService = vecVocabService;
		this.tokenizer = tokenizer;
		this.indexDir = indexDir;
		this.wordVectorfile = wordVectorFile;
		this.tfIdfService = tfIdfService;
	}
	
	public void process() throws IOException {
		VectorVocab vecVocab = vecVocabService.loadVectorVocab(wordVectorfile, indexDir, true);
		TfIdfInfo tfIdfInfo = tfIdfService.computeTfIdfRecursively(new File(wordsPath).toPath(), true);
		DocumentProcessorNToOne docProcessor = new SSenseContextProcessor(vecVocab, tokenizer, tfIdfInfo);
		RecursiveTransformer.recursiveProcess(new File[]{new File(wordsPath), new File(ssensesInputPath)}, new File(ssenseOutPath), docProcessor, 1.0);
	}

}
