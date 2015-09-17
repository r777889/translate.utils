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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qsptools.translator.bean.DicoEntry;
import qsptools.translator.model.DicoTableModel;
import qsptools.translator.utils.TranslatorUtils;
import translator.enums.ELanguage;
import translator.service.get.impl.GoogleTranslation;

public class Tokenise {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		File dictionary = new File("lust.dict.xml");
		DicoTableModel model = new DicoTableModel();
		TranslatorUtils.fillDico(model, dictionary);

		Map<String, String> cyrillicText = new HashMap<>();

		for (DicoEntry entry : model.items) {
			if (entry.getTranslated() != null && !entry.getTranslated().isEmpty()) {
				cyrillicText.put(entry.getOriginal(), entry.getTranslated());
			}
		}

		String content = "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream("Lust_1.6.html"))) {
			byte[] buf = new byte[1024];
			int count = 0;
			while ((count = bis.read(buf)) != -1) {
				baos.write(buf, 0, count);
			}
		}
		content = new String(baos.toByteArray(), "UTF-8");


		List<String> possibleTextList = new ArrayList<>(Arrays.asList(content.split("\\\"|\"|'")));
		for (String possibleText : possibleTextList) {
			String[] parts = possibleText.split("(</?.*?>|&.*?;|\\$\\w+\\[\\w*\\])");

			for (String part : parts) {
				String result = part.trim();
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
									String[] parts2 = result.split("\\.|\\?|!");
									for (String part2 : parts2) {
										String result2 = part2.trim();
										if (!cyrillicText.containsKey(result2)) {
											cyrillicText.put(result2, null);
										}
									}
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

		GoogleTranslation translation = new GoogleTranslation();

		for (Map.Entry<String, String> entry : cyrillicText.entrySet()) {
			if (entry.getValue() == null || entry.getValue().isEmpty()) {
				System.out.println("translating " + entry.getKey());
				entry.setValue(translation.translate(entry.getKey(), ELanguage.RUSSIAN, ELanguage.ENGLISH, null));
			}
		}

		TranslatorUtils.persistDico(cyrillicText, dictionary);

		List<String> originals = new ArrayList<>(cyrillicText.keySet());
		originals.sort(new SizeComparator());

		for (String original : originals) {
			content = content.replace(original, cyrillicText.get(original));
		}

		try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("Lust_en.html"), "UTF-8")) {
			osw.write(content);
		}

	}

}
