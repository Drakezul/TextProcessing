package kontoauszuege;

import java.io.File;
import java.util.Comparator;

public class KontoauszugComparator implements Comparator<File> {
	
	@Override
	public int compare(File arg0, File arg1) {
		if (!arg0.getPath().equals(arg1.getPath())) {
			int dateArg0 = Integer.parseInt(arg0.getName().substring(28, 32) + arg0.getName().substring(25, 28));
			int dateArg1 = Integer.parseInt(arg1.getName().substring(28, 32) + arg1.getName().substring(25, 28));
			return dateArg0 - dateArg1;
		} else {
			return 0;
		}
	}
}