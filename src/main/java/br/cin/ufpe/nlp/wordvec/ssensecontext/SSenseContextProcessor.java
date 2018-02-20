package br.cin.ufpe.nlp.wordvec.ssensecontext;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;

import br.cin.ufpe.nlp.api.bagofwords.TfIdfInfo;
import br.cin.ufpe.nlp.api.tokenization.Tokenizer;
import br.cin.ufpe.nlp.api.tokenization.TokenizerFactory;
import br.cin.ufpe.nlp.api.transform.DocumentProcessorNToOne;
import br.cin.ufpe.nlp.api.vectors.VectorVocab;

public class SSenseContextProcessor implements DocumentProcessorNToOne {
	private VectorVocab vecVocab;
	private TokenizerFactory<Tokenizer<String>> tokenizer;
	private TfIdfInfo tfIdfInfo;
	private long totalDocs;
	private int rareWordThreshold = 3;


	public SSenseContextProcessor(VectorVocab vecVocab, TokenizerFactory<Tokenizer<String>> tokenizer, TfIdfInfo tfIdfInfo) throws IOException {
		this.vecVocab = vecVocab;
		this.tokenizer = tokenizer;
		this.tfIdfInfo = tfIdfInfo;
		this.totalDocs = tfIdfInfo.totalNumberOfDocs();
	}

	@Override
	public void processDocument(Reader[] inputDocuments, Writer outputDocument) throws IOException {
		assert(inputDocuments.length == 2);	//should be words followed by supersenses
		Tokenizer<String> wordTokenizer = tokenizer.create(inputDocuments[0]);
		Tokenizer<String> ssenseTokenizer = tokenizer.create(inputDocuments[1]);
		double[] sSenseContext = new double[this.vecVocab.embedSize()];
		double weightSum = 0;
		while(wordTokenizer.hasMoreTokens()) {
			String word = wordTokenizer.nextToken().toLowerCase(Locale.US);
			String ssense = ssenseTokenizer.nextToken();
			if (ssense.trim().length() >= 2) {
				float[] embed;
				if ((embed = vecVocab.embeddingFor(word)) != null) {
					final double wordAppearsInNdocs = tfIdfInfo.docAppearedIn(word);
					if (wordAppearsInNdocs >= rareWordThreshold) {
						final double idfWeight = Math.log(this.totalDocs / wordAppearsInNdocs);
						if (Double.isNaN(idfWeight) || Double.isInfinite(idfWeight)) {
							System.err.println("oops, idfWeight was invalid");
						}
						weightSum += idfWeight;
						assert (sSenseContext.length == embed.length);
						for (int i = 0; i < sSenseContext.length; i++) {
							sSenseContext[i] += embed[i] * idfWeight;
						}
					}
				}
			}
		}
		if (weightSum != 0) {
			for (int i = 0; i < sSenseContext.length; i++) {
				sSenseContext[i] /= weightSum;
			}
		}
		outputDocument.write(Double.toString(sSenseContext[0]));
		for (int i = 1; i < sSenseContext.length; i++) {
			outputDocument.write(' ');
			outputDocument.write(Double.toString(sSenseContext[i]));
			if (Double.isNaN(sSenseContext[i]) || Double.isInfinite(sSenseContext[i])) {
				System.err.println("oops, globalContext[i] was invalid");
			}
			
		}
		outputDocument.write('\n');
		outputDocument.flush();
		

	}

}
