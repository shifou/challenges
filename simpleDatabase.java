import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Thumbtack challenge 
 * idea: for best performance consideration and meet the
 * command function fully not like "real" database design this database very
 * naive auto-commit each command and store the undo list for each transaction
 * rollback according to the undo list. 
 * Complie Env: 
 * 		JavaSE-1.7 
 * Complie Command:
 * 		javac simpleDatabase.java 
 * Run: 
 * 		java simpleDatabase < in.txt > out.txt
 * 
 * @author Lixun Mao, Carnegie Mellon University
 **/

public class simpleDatabase {
	// store Key Value pair
	private HashMap<String, String> values;
	// store Value Count pair
	private HashMap<String, Integer> counts;
	// store transaction list
	private ArrayList<transaction> trans;
	// check whether there is a transaction
	private boolean transBlockOn;

	private class transaction {
		// store the original value of the key for the rollback
		private HashMap<String, String> oriVals;

		public transaction() {
			oriVals = new HashMap<String, String>();
		}
	}

	public simpleDatabase() {
		values = new HashMap<String, String>();
		counts = new HashMap<String, Integer>();
		trans = new ArrayList<transaction>();
		transBlockOn = false;
	}

	private void start() {
		// receive from CMD until "END"
		Scanner command = new Scanner(System.in);
		while (command.hasNext()) {
			String hold = command.nextLine();
			if (hold.equals("END")) {
				return;
			} else
				System.out.println(execute(hold));
		}
	}

	// O(1), set the value at the same time while update the counts for
	// NUMTOEQUAL use
	private String set(String cm1, String cm2) {
		if (this.transBlockOn == false) {
			if (values.containsKey(cm1)) {
				String oriVal = values.get(cm1);
				delCount(oriVal);
				values.put(cm1, cm2);
				addCount(cm2);
			} else {
				values.put(cm1, cm2);
				addCount(cm2);
			}
		} else {
			int pos = this.trans.size() - 1;
			// if first time we want to modify this key in the current tranction
			// we need to record the original value in order to rollback
			if (this.trans.get(pos).oriVals.containsKey(cm1) == false) {
				// if current database has not this key,
				// but we set this key in the current transaction
				// which means we need to delete when rollback
				// store in the format #V, V is used to update the counts when
				// rollback
				if (values.containsKey(cm1) == false) {
					this.trans.get(pos).oriVals.put(cm1, "#" + cm2);
					values.put(cm1, cm2);
					addCount(cm2);
				} else {
					// otherwise current database has this key
					// we need to record the original value when rollback
					String oriVal = values.get(cm1);
					this.trans.get(pos).oriVals.put(cm1, oriVal);
					delCount(oriVal);
					values.put(cm1, cm2);
					addCount(cm2);
				}
			}
			// not the first time set this key, we already knew the original
			// just set this key value as usual
			else {
				if (values.containsKey(cm1)) {
					String oriVal = values.get(cm1);
					delCount(oriVal);
					values.put(cm1, cm2);
					addCount(cm2);
				} else {
					values.put(cm1, cm2);
					addCount(cm2);
				}
			}
		}
		return "";
	}

	// O(1), delete from Hash

	private String unset(String cm1) {
		if (this.transBlockOn == false) {
			if (values.containsKey(cm1)) {
				String oriVal = values.get(cm1);
				delCount(oriVal);
				values.remove(cm1);
			}
		} else {
			int pos = this.trans.size() - 1;
			// if first time accessing this key
			if (this.trans.get(pos).oriVals.containsKey(cm1) == false) {
				// if current database has not this key, we do nothing
				// but if current database has this key,
				// and we unset this key in the current transaction
				// which means we need to set back when rollback
				if (values.containsKey(cm1)) {
					String oriVal = values.get(cm1);
					this.trans.get(pos).oriVals.put(cm1, oriVal);
					delCount(oriVal);
					values.remove(cm1);
				}
			}
			// we already record the original value
			// take it easy and delete directly
			else {
				String oriVal = values.get(cm1);
				delCount(oriVal);
				values.remove(cm1);
			}
		}
		return "";
	}

	// O(1) just clear the transaction list
	private boolean commit() {
		if (this.transBlockOn) {
			trans.clear();
			this.transBlockOn = false;
			return true;
		} else {
			return false;
		}
	}

	// O(1) delete the number equal to the specific value
	private void delCount(String val) {
		Integer temp = counts.get(val);
		if (temp == 1)
			counts.remove(val);
		else
			counts.put(val, temp - 1);
	}

	// O(1) add the number equal to the specific value
	private void addCount(String val) {
		if (counts.containsKey(val))
			counts.put(val, counts.get(val) + 1);
		else
			counts.put(val, 1);
	}

	// O(M) where M is the undo list size of current transaction
	private boolean rollback() {
		if (this.transBlockOn == false)
			return false;
		int pos = trans.size() - 1;
		for (String key : trans.get(pos).oriVals.keySet()) {
			String oriVal = trans.get(pos).oriVals.get(key);
			// #V means in previous transaction
			// the key is unset and we need to delete that V
			if (oriVal.startsWith("#")) {
				oriVal = oriVal.substring(1);
				delCount(oriVal);
				values.remove(key);
				continue;
			}
			// change to the previous value
			if (values.containsKey(key)) {
				String curVal = values.get(key);
				delCount(curVal);
				values.put(key, oriVal);
				addCount(oriVal);
			} else {
				values.put(key, oriVal);
				addCount(oriVal);
			}
		}
		trans.remove(pos);
		if (pos == 0)
			this.transBlockOn = false;
		return true;
	}

	private String execute(String nextLine) {
		String[] cm = nextLine.split(" ");
		switch (cm[0]) {
		// new transaction
		case "BEGIN":
			this.transBlockOn = true;
			transaction hold = new transaction();
			this.trans.add(hold);
			return "";
			// O(1)
		case "GET":
			if (values.containsKey(cm[1])) {
				return values.get(cm[1]);
			} else
				return "NULL";
		case "SET":
			return set(cm[1], cm[2]);
		case "UNSET":
			return unset(cm[1]);
			// O(1), return answer from Hash
		case "NUMEQUALTO":
			if (counts.containsKey(cm[1]) == false) {
				return "0";
			} else
				return counts.get(cm[1]).toString();
		case "COMMIT":
			if (commit())
				return "";
			else
				return "NO TRANSACTION";
		case "ROLLBACK":
			if (rollback())
				return "";
			else
				return "NO TRANSACTION";
		default:
			return "";
		}
	}

	// start
	public static void main(String[] args) {
		simpleDatabase sd = new simpleDatabase();
		sd.start();
	}
}
