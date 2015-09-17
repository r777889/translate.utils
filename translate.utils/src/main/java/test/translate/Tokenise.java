package test.translate;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import qsptools.translator.bean.DicoEntry;
import qsptools.translator.model.DicoTableModel;
import qsptools.translator.utils.TranslatorUtils;
import translator.enums.ELanguage;
import translator.service.get.impl.GoogleTranslation;

public class Tokenise {

	private File dictionary;
	private File original;
	private File translatedOutputFile;

	public Tokenise(CommandLine commandLine) {
		dictionary = new File(commandLine.getOptionValue("d"));
		original = new File(commandLine.getOptionValue("i"));
		translatedOutputFile = new File(commandLine.getOptionValue("o"));
	}

	private void go() throws IOException {
		Map<String, String> cyrillicText = loadDictionary(dictionary);

		String content = loadOriginal(original);

		getTokens(cyrillicText, content);

		String translatedText = translate(cyrillicText, content);

		TranslatorUtils.persistDico(cyrillicText, dictionary);

		saveTranslated(translatedOutputFile, translatedText);
	}

	private static Options getCommandLineOptions() {
		Options opts = new Options();
		opts.addOption(Option.builder("d").argName("dictionary").required().hasArg().build());
		opts.addOption(Option.builder("i").argName("input").required().hasArg().build());
		opts.addOption(Option.builder("o").argName("output").required().hasArg().build());
		return opts;
	}

	private Map<String, String> loadDictionary(File dictionary) {
		Map<String, String> cyrillicText = new HashMap<>();

		DicoTableModel model = new DicoTableModel();
		TranslatorUtils.fillDico(model, dictionary);
		for (DicoEntry entry : model.items) {
			if (entry.getTranslated() != null && !entry.getTranslated().isEmpty()) {
				cyrillicText.put(entry.getOriginal(), entry.getTranslated());
			}
		}
		return cyrillicText;
	}

	private String loadOriginal(File original) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(original))) {
			byte[] buf = new byte[1024];
			int count = 0;
			while ((count = bis.read(buf)) != -1) {
				baos.write(buf, 0, count);
			}
		}
		return new String(baos.toByteArray(), "UTF-8");
	}

	private void getTokens(Map<String, String> cyrillicText, String content) {
		Queue<String> possibleTextList = new ConcurrentLinkedQueue<>(
				Arrays.asList(content.split("\\\"|\"|'|</?.*?>|&.*?;|\\$\\w+\\[\\w*\\]|\\n|\\r|;|\\-|\\{|\\}|:")));
		for (String possibleText : possibleTextList) {
			String result = possibleText.trim();
			if (result.startsWith(">")) {
				result = result.substring(1);
			}
			if (result.endsWith("\\")) {
				result = result.substring(0, result.length() - 1);
			}
			if (result.length() > 2) {
				for (char c : result.toCharArray()) {
					if (c > 1024 && c < 1279) {
						if (!cyrillicText.containsKey(result)) {
							// deal with large sentences
							if (result.length() > 256) {
								possibleTextList.addAll(Arrays.asList(result.split("\\.|\\?|!")));
							} else {
								cyrillicText.put(result, null);
							}
						}
						break;
					}
				}
			}
		}
	}

	private String translate(Map<String, String> cyrillicText, String content) {
		GoogleTranslation translation = new GoogleTranslation();

		for (Map.Entry<String, String> entry : cyrillicText.entrySet()) {
			if (entry.getValue() == null || entry.getValue().isEmpty()) {
				System.out.println("translating " + entry.getKey());
				entry.setValue(translation.translate(entry.getKey(), ELanguage.RUSSIAN, ELanguage.ENGLISH, null));
			}
		}

		List<String> originals = new ArrayList<>(cyrillicText.keySet());
		originals.sort(new SizeComparator());

		for (String original1 : originals) {
			content = content.replace(original1, cyrillicText.get(original1));
		}
		return content;
	}

	private void saveTranslated(File translatedOutputFile, String translatedText) throws IOException {
		try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(translatedOutputFile), "UTF-8")) {
			osw.write(translatedText);
		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Options opts = getCommandLineOptions();
		try {
			CommandLine commandLine = new DefaultParser().parse(opts, args);
			new Tokenise(commandLine).go();
		} catch (ParseException e) {
			new HelpFormatter().printHelp("java -jar translate.utils.jar", opts);
		}
	}
}
