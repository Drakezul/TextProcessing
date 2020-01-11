package bankStatements;

import java.io.File;
import java.util.Comparator;

public class BankStatementComparator implements Comparator<File> {

	@Override
	public int compare(File arg0, File arg1) {
		if (!arg0.getPath().equals(arg1.getPath())) {
			short v1YearStart = 28;
			short v1NumberStart = 25;
			short v2YearStart = 9;
			short v2NumberStart = 17;
			int dateArg0;
			if (arg0.getName().startsWith("Kontoauszug")) {
				dateArg0 = Integer.parseInt(arg0.getName().substring(v1YearStart, v1YearStart + 4)
						+ arg0.getName().substring(v1NumberStart, v1NumberStart + 3));
			} else {
				dateArg0 = Integer.parseInt(arg0.getName().substring(v2YearStart, v2YearStart + 4)
						+ arg0.getName().substring(v2NumberStart, v2NumberStart + 3));
			}
			int dateArg1;
			if (arg1.getName().startsWith("Kontoauszug")) {
				dateArg1 = Integer.parseInt(arg1.getName().substring(v1YearStart, v1YearStart + 4)
						+ arg1.getName().substring(v1NumberStart, v1NumberStart + 3));
			} else {
				dateArg1 = Integer.parseInt(arg1.getName().substring(v2YearStart, v2YearStart + 4)
						+ arg1.getName().substring(v2NumberStart, v2NumberStart + 3));
			}
			return dateArg0 - dateArg1;
		} else {
			return 0;
		}
	}
}