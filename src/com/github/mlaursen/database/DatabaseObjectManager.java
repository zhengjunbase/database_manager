package com.github.mlaursen.database;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mlaursen.annotations.DatabaseAnnotationType;
import com.github.mlaursen.annotations.DatabaseField;
import com.github.mlaursen.annotations.MultipleDatabaseField;
import com.github.mlaursen.database.objects.DatabaseObject;
import com.github.mlaursen.database.objecttypes.Createable;
import com.github.mlaursen.database.objecttypes.Deleteable;
import com.github.mlaursen.database.objecttypes.Filterable;
import com.github.mlaursen.database.objecttypes.GetAllable;
import com.github.mlaursen.database.objecttypes.Getable;
import com.github.mlaursen.database.objecttypes.NoCursor;
import com.github.mlaursen.database.objecttypes.Updateable;

/**
 * A Database Manager for a database object.
 * This generates a Database Package with Database Procedures.
 * For each Interface that the Database object implements, it will create
 * a procedure within the package.
 * 
 * @see DatabasePackage
 * @see DatabaseProcedure
 * @see Getable
 * @see GetAllable
 * @see Updateable
 * @see Deleteable
 * @see Filterable
 * @author mikkel.laursen
 *
 */
public class DatabaseObjectManager {

	private Manager manager;
	private Class<? extends DatabaseObject> type;
	private DatabasePackage pkg;
	private List<String> availableCalls = new ArrayList<String>();
	public DatabaseObjectManager(Class<? extends DatabaseObject> class1) {
		type = class1;
		manager = new Manager();
		pkg = new DatabasePackage(class1);
		generatePackageProcedures();
	}

	
	/**
	 * This adds each procedure to the package for every
	 * database objecttype that the database object implments.
	 * It would be nice to just dynamically create for every class
	 * in: com.github.mlaursen.database.objecttypes
	 * 
	 */
	private void generatePackageProcedures() {
		createProcedure(Getable.class);
		createProcedure(GetAllable.class);
		createProcedure(Createable.class);
		createProcedure(Deleteable.class);
		createProcedure(Filterable.class);
		createProcedure(Updateable.class);
	}
	
	/**
	 * A check if the procedure exists within the database object package
	 * @param procedureName
	 * @return
	 */
	public boolean canCall(String procedureName) {
		return availableCalls.contains(procedureName);
	}
	
	/**
	 * Executes a database stored procedure that does not have a result set as a result.
	 * The return type will be a boolean if the stored procedure affected at least 1 row.
	 * Before executing the stored procedure, a check is performed to see if that procedure exists
	 * in the package, if it does not exist, false is returned and an error message is printed
	 * to the System.err.
	 * 
	 * @param procedureName The procedure name to lookup and call from the Database Object's 
	 * 	package
	 * @param An array of object parameters to be passed to the stored procedure. 
	 * 	The parameters will be cast to their corresponding types. {@link com.github.mlaursen.database.Manager}
	 * @return A boolean of success for the stored procedure call. If at least 1 row was affected, 
	 * 	the result is true.
	 */
	public boolean executeStoredProcedure(String procedureName, Object... parameters) {
		if(canCall(procedureName)) {
			return manager.executeStoredProcedure(pkg, procedureName, parameters);
		}
		else {
			System.err.println(pkg.getName() + " does not contain the procedure " + procedureName);
			return false;
		}
	}
	
	/**
	 * Executes a database stored procedure and returns a list of result rows.
	 * Before executing the stored procedure, a check is performed to see if that procedure exists
	 * in the package, if it does not exist, an empty ResultSet is returned and an error message is printed
	 * to the System.err.
	 * 
	 * @param procedureName The procedure name to lookup and call from the Database Object's 
	 * 	package
	 * @param An array of object parameters to be passed to the stored procedure. 
	 * 	The parameters will be cast to their corresponding types. {@link com.github.mlaursen.database.Manager}
	 * @return a MyResultSet for the resulting rows from the database call. @see MyResultSet
	 */
	public MyResultSet executeCursorProcedure(String procedureName, Object... parameters) {
		if(canCall(procedureName)) {
			return manager.executeCursorProcedure(pkg, procedureName, parameters);
		}
		else {
			System.err.println(pkg.getName() + " does not contain the procedure " + procedureName);
			return new MyResultSet(Arrays.asList(new MyResultRow()));
		}
	}
	
	/**
	 * Executes a database stored procedure and returns only the first row.
	 * Mostly used for looking up a single object from the database
	 * @see executeCursorProcedure
	 * 
	 * @param procedureName	The procedure name to lookup and call from the Database Object's 
	 * 	package
	 * @param parameters An array of object parameters to be passed to the stored procedure. 
	 * 	The parameters will be cast to their corresponding types. {@link com.github.mlaursen.database.Manager}
	 * @return a MyResultRow for the resulting row from the database call. @see MyResultRow
	 */
	public MyResultRow getFirstRowFromCursorProcedure(String procedureName, Object... parameters) {
		return executeCursorProcedure(procedureName, parameters).getRow();
	}
	
	/**
	 * Checks if the type instance variable is assignable from a given class
	 * @param c	Class to test for
	 * @return
	 */
	private boolean typeIsOf(Class<?> c) {
		return c.isAssignableFrom(type);
	}
	
	/**
	 * Creates a procedure in the database object package.
	 * 
	 * @param c		This is a class within the com.github.database.objecttypes package.
	 * 	It creates a procedure based on the lowercase version of the class name and removes
	 * the 'able' from the name.
	 */
	private void createProcedure(Class<?> c) {
		if(typeIsOf(c)) {
			String procName = c.getSimpleName().toLowerCase().replace("able", "");
			availableCalls.add(procName);
			DatabaseProcedure proc = new DatabaseProcedure(procName, getParametersFromClass(c));
			if(NoCursor.class.isAssignableFrom(c)) {
				proc.setHasCursor(false);
			}
			if(c.equals(GetAllable.class)) {
				proc.setDisplayName("getall");
				proc.setName("get");
			}
			pkg.addProcedure(proc);
		}
	}
	
	private String[] getParametersFromClass(Class<?> c) {
		return getParametersFromClass(DatabaseAnnotationType.classToType(c), type);
	}
	
	
	private String[] getParametersFromClass(DatabaseAnnotationType proc, Class<?> c) {
		Map<Integer, String> map = getParametersFromClass(proc, c, new HashMap<Integer, String>(), 0);
		int s = map.size();
		String[] ps = new String[s];
		for(int i = 0; i < s; i++) {
			ps[i] = map.get(i);
		}
		return ps;
	}
	
	/**
	 * Phew. Big handler.
	 * Recursive method to check a class and all super classes for the Annotations for a database.
	 * 
	 * @param proc Procedure type to lookup and possibly add parameters to the results 
	 * @param c	A class to check for annotations
	 * @param current	A result set
	 * @param counter	Interger for the position to place the field in the procedure string
	 * @return
	 */
	private Map<Integer, String> getParametersFromClass(DatabaseAnnotationType proc, Class<?> c, Map<Integer, String> current, int counter) {
		if(c.equals(Object.class)) {
			return current;
		}
		else {
			for(Field f : c.getDeclaredFields()) {
				if(f.isAnnotationPresent(MultipleDatabaseField.class)) {
					MultipleDatabaseField m = f.getAnnotation(MultipleDatabaseField.class);
					if(Arrays.asList(m.values()).contains(proc)) {
						for(String n : m.names()) {
							current.put(counter, n);
							counter++;
						}
					}
				}
				else if(f.isAnnotationPresent(DatabaseField.class)) {
					DatabaseField a = f.getAnnotation(DatabaseField.class);
					if(Arrays.asList(a.values()).contains(proc)) {
						try {
							int pos;
							if(a.reorder()) {
								pos = -1;
								if(proc.equals(DatabaseAnnotationType.GET))
									pos = a.getPosition();
								else if(proc.equals(DatabaseAnnotationType.GETALL))
									pos = a.getAllPosition();
								else if(proc.equals(DatabaseAnnotationType.CREATE))
									pos = a.createPosition();
								else if(proc.equals(DatabaseAnnotationType.DELETE))
									pos = a.deletePosition();
								else if(proc.equals(DatabaseAnnotationType.FILTER))
									pos = a.filterPosition();
							}
							else {
								pos = counter;
							}
							counter++;
							if(pos == -1) {
								throw new Exception();
							}
							else {
								current.put(pos, f.getName());
							}
						}
						catch (Exception e) {
							System.err.println("The position for " + proc + " has not been initialized.");
							System.err.println("This field[" + f.getName() + "]'s value was not added to the results");
						}
					}
				}
			}
			return getParametersFromClass(proc, c.getSuperclass(), current, counter);
		}
	}
	
	/**
	 * 
	 * @return The package for the databaseobject
	 */
	public DatabasePackage getPackage() {
		return this.pkg;
	}
	
	@Override
	public String toString() {
		return "DatabaseObjectManager [pkg=" + pkg + ", type=" + type.getSimpleName()
				+ "]";
	}
}
