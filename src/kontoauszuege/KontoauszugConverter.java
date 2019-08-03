package kontoauszuege;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Arrays;

public class KontoauszugConverter {

	public static void main(String[] args) {
		new KontoauszugConverter(args[0]);
	}

	public static final String ANTICIPATED_COLUMNS = "Bu-Tag;Wert;Vorgang;Sender/Empfänger;Additionals;IBAN;Betrag;";

	public KontoauszugConverter(String folderPath) {
		File[] files = getFiles(folderPath);
		Arrays.sort(files, new KontoauszugComparator());

		KontoauszugConverterToCSV[] instances = createInstances(files);
		Thread[] threads = startInstancesInThreads(instances);
		printAnticipatedColumns();
		write(folderPath, instances, threads);
	}

	private void write(String folderPath, KontoauszugConverterToCSV[] instances, Thread[] threads) {
		try {
			OutputStreamWriter writer = new OutputStreamWriter(
					new FileOutputStream(folderPath + "/Zusammenfassung.csv"),
					Charset.forName("Windows-1252").newEncoder());
			writer.append(ANTICIPATED_COLUMNS + "\n");

			double sum = 0;
			DecimalFormat decimalFormat = new DecimalFormat("0.00");
			for (int i = 0; i < instances.length; i++) {
				try {
					threads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for (String[] currentSet : instances[i].results) {
					writeCurrentSetAsCSV(writer, currentSet);
					sum += extractAmount(currentSet[currentSet.length - 1]);
					writer.append(decimalFormat.format(sum) + ";\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private File[] getFiles(String path) {
		File resourcesFolder = new File(path);
		return resourcesFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isFile() && file.getPath().endsWith(".txt");
			}
		});
	}

	private void printAnticipatedColumns() {
		String[] anticipatedColumns = ANTICIPATED_COLUMNS.split(";");
		int setLength = ANTICIPATED_COLUMNS.length();
		System.out.println("Set length: " + setLength);
		System.out.println("Anticipated sets: ");
		for (String column : anticipatedColumns) {
			System.out.println(column);
		}
	}

	private Thread[] startInstancesInThreads(KontoauszugConverterToCSV[] instances) {
		Thread[] threads = new Thread[instances.length];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(instances[i]);
			threads[i].start();
		}
		return threads;
	}

	private KontoauszugConverterToCSV[] createInstances(File[] files) {
		KontoauszugConverterToCSV[] instances = new KontoauszugConverterToCSV[files.length];
		for (int i = 0; i < instances.length; i++) {
			instances[i] = new KontoauszugConverterToCSV(files[i].getPath());
		}
		return instances;
	}

	private void writeCurrentSetAsCSV(OutputStreamWriter writer, String[] currentSet) throws IOException {
		for (int j = 0; j < currentSet.length; j++) {
			String currentColumn = currentSet[j];
			if (currentColumn == null) {
				writer.append(";");
			} else {
				writer.append(currentColumn + ";");
			}

		}
	}

	private double extractAmount(String amount) {
		double deltaValue = 0;
		// remove H, S, whitespaces and dots and convert commas to dots
		String value = amount.replaceAll("H", "")
				.replaceAll("S", "")
				.replaceAll("\\s", "")
				.replaceAll("\\.", "")
				.replaceAll(",", ".");
		if (amount.endsWith("H")) {
			deltaValue = Double.parseDouble(value);
		} else if (amount.endsWith("S")) {
			deltaValue = (-1) * Double.parseDouble(value);
		}
		return deltaValue;
	}

}
