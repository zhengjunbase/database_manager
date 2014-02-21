package com.github.mlaursen.database.objects;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mlaursen.annotations.DatabaseField;
import com.github.mlaursen.annotations.DatabaseFieldType;
import com.github.mlaursen.database.ObjectManager;
import com.github.mlaursen.database.Util;

/**
 * Basic outline for a DatbaseObject. Every database object must have at least a
 * primary key
 * 
 * @author mikkel.laursen
 * 
 */
public abstract class DatabaseObject {

	protected final ObjectManager manager = createManager();
	@DatabaseField(values = { DatabaseFieldType.GET, DatabaseFieldType.UPDATE })
	protected String primaryKey;
	protected String primaryKeyName = "id";

	/**
	 * This is mostly used to access the ObjectManager to do Database calls
	 */
	public DatabaseObject() {}

	/**
	 * Create a database object by giving it a primary key and then searching
	 * for it in the database. It then calls all the setters for that database
	 * object where the setter has a MyResultRow as the only parameter
	 * 
	 * @param primaryKey
	 */
	public DatabaseObject(String primaryKey) {
		init(primaryKey);
	}

	/**
	 * Create a database object by giving it a primary key and then searching
	 * for it in the database. It then calls all the setters for that database
	 * object where the setter has a MyResultRow as the only parameter
	 * 
	 * @param primaryKey
	 */
	public DatabaseObject(Integer primaryKey) {
		this(primaryKey.toString());
	}

	/**
	 * Sets the primary key to the database column described as the
	 * primaryKeyName. The default is 'id'
	 * 
	 * @param r
	 */
	public DatabaseObject(MyResultRow r) {
		setAll(r);
	}

	/**
	 * 
	 * @param primaryKey
	 */
	protected void init(String primaryKey) {
		MyResultRow r = manager.getFirstRowFromCursorProcedure("get", primaryKey);
		setAll(r);
	}

	/**
	 * This finds all the methods that start with 'set' and have a single
	 * parameter of a MyResultRow and then invokes that method.
	 * 
	 * @param r
	 */
	protected void setAll(MyResultRow r) {
		Method[] methods = this.getClass().getMethods();
		for (Method m : methods) {
			if (m.getName().startsWith("set") && Arrays.asList(m.getParameterTypes()).contains(MyResultRow.class)) {
				try {
					m.setAccessible(true);
					m.invoke(this, r);
					m.setAccessible(false);
				}
				catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
					System.err.println("There was a problem trying to invoke " + m.getName());
				}
			}
		}
	}

	/**
	 * 
	 * @param primaryKey
	 */
	public void setPrimaryKey(String primaryKey) {
		this.primaryKey = primaryKey;
	}

	/**
	 * 
	 * @param primaryKey
	 */
	public void setPrimaryKey(Integer primaryKey) {
		this.primaryKey = primaryKey.toString();
	}

	/**
	 * Sets the primary key to the database column described as the
	 * primaryKeyName. The default is 'id'
	 * 
	 * @param r
	 */
	public void setPrimaryKey(MyResultRow r) {
		primaryKey = r.get(primaryKeyName);
	}

	/**
	 * Get the primaryKey value
	 * 
	 * @return
	 */
	public String getPrimaryKey() {
		return primaryKey;
	}

	/**
	 * Set the primary key name to the new string given. This will be used for
	 * initializing a database object
	 * 
	 * @param name
	 */
	public void setPrimaryKeyName(String name) {
		primaryKeyName = name;
	}

	/**
	 * Returns the primary key name for the database object. The default is 'id'
	 * 
	 * @return
	 */
	public String getPrimaryKeyName() {
		return primaryKeyName;
	}

	/**
	 * Creates a manager for the database object.
	 * {@link com.github.mlaursen.database.ObjectManager} Basic implementation
	 * is:
	 * 
	 * @return new ObjectManager(this.getClass());
	 */
	protected ObjectManager createManager() {
		return new ObjectManager(this.getClass());
	}

	/**
	 * Debug tool to see what DatabaseManager has been generated
	 * 
	 * @return
	 */
	public String getDatabaseManagerToString() {
		return manager.toString();
	}

	/**
	 * Get an array of Object to be passed to a database procedure call. The
	 * array is generated by seraching for all the DatabaseField or
	 * MultipleDatabaseField annotations located in the class starting with the
	 * DatabaseObject and working down to the current class.
	 * 
	 * @param proc
	 *            The procedure to get the parameters for
	 * @return
	 */
	private Object[] getParameters(DatabaseFieldType proc) {
		return getParameters(getParametersMap(proc));
	}

	/**
	 * Takes a Map of interger position and objects and sorts them from 0 - max
	 * size of map into an array of objects
	 * 
	 * @param map
	 * @return
	 */
	private Object[] getParameters(Map<Integer, Object> map) {
		int s = map.size();
		Object[] ps = new Object[s];
		for (int i = 0; i < s; i++) {
			ps[i] = map.get(i);
		}
		return ps;
	}

	/**
	 * Takes in a DatabaseFieldType and generates a Map of Integer, Object pairs
	 * to be passed to the database stored procedure.
	 * 
	 * @param proc
	 * @return
	 */
	private Map<Integer, Object> getParametersMap(DatabaseFieldType proc) {
		int counter = 0;
		Map<Integer, Object> params = new HashMap<Integer, Object>();
		List<Class<?>> classes = Util.getClassList(this.getClass());
		for (Class<?> c : classes) {
			for (Field f : c.getDeclaredFields()) {
				f.setAccessible(true);
				if (f.isAnnotationPresent(DatabaseField.class)) {
					DatabaseField a = f.getAnnotation(DatabaseField.class);
					if (Arrays.asList(a.values()).contains(proc)) {
						try {
							Object o = f.get(this);
							int pos = a.reorder() ? DatabaseFieldType.getPosition(proc, a) : counter;
							counter++;
							if (pos == -1) {
								throw new Exception();
							}
							else {
								params.put(pos, o);
							}
						}
						catch (IllegalArgumentException | IllegalAccessException e) {
							e.printStackTrace();
						}
						catch (Exception e) {
							String err = "The position for the procedure '" + proc + "' has not been initialized for the field " + "["
									+ f.getName() + "]\nin class [" + c.getName() + "].  This error occured when seraching for the values "
									+ "to add when calling the stored procedure. The value has not been added to the parameter map.";
							System.err.println(err);
						}
					}
				}
				f.setAccessible(false);
			}
		}
		return params;
	}

	/**
	 * This is a default implementation for the Getable Interface get Method. If
	 * the database object is not Getable, it will return null
	 * 
	 * @param primaryKey
	 *            The primary key to search for in the database
	 * @return Either a null DatabaseObject or a result DatabaseObjet
	 */
	public DatabaseObject get(String primaryKey) {
		return manager.executeCursorProcedure("get", primaryKey).getRow().construct(this.getClass());
	}

	/**
	 * This is a implementation for using generics if you need a specific
	 * Database Object sub class
	 * 
	 * @param primaryKey
	 * @param type
	 *            Sub class to cast to
	 * @return
	 */
	public <T extends DatabaseObject> T get(String primaryKey, Class<T> type) {
		return manager.executeCursorProcedure("get", primaryKey).getRow().construct(type);
	}

	/**
	 * This is a default implementation for the GetAllable Interface 'getAll'
	 * method. If the database object is not GetAllable, it will return an empty
	 * list. If you want an additional key to be applied to the getAll method,
	 * add the Annotation @DatabaseField(values={DatabaseField.GETALL},
	 * position={$positionToBeApplied})
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DatabaseObject> getAll() {
		Object[] params = getParameters(DatabaseFieldType.GETALL);
		return (List<DatabaseObject>) manager.executeCursorProcedure("getall", params).toListOf(this.getClass());
	}

	/**
	 * This is a implementation for using generics if you need a specific list
	 * of Database Object sub class. Searches for all DatabaseField annotations
	 * and calls the getters in order as an array of objects to be passed to the
	 * delete procedure.
	 * 
	 * @param primaryKey
	 * @param type
	 *            Sub class to cast to
	 * @return
	 */
	public <T extends DatabaseObject> List<T> getAll(Class<T> type) {
		Object[] params = getParameters(DatabaseFieldType.GETALL);
		return manager.executeCursorProcedure("getall", params).toListOf(type);
	}

	/**
	 * Default implementation for a DatabaseObject that is createable.. Searches
	 * for all DatabaseField annotations and calls the getters in order as an
	 * array of objects to be passed to the delete procedure.
	 * 
	 * @return
	 */
	public boolean create() {
		Object[] params = getParameters(DatabaseFieldType.NEW);
		for(Object p : params) {
			if(p == null)
				return false;
		}
		return manager.executeStoredProcedure("new", params);
	}

	/**
	 * Default implmenetation for a database object that is filterable..
	 * Searches for all DatabaseField annotations and calls the getters in order
	 * as an array of objects to be passed to the delete procedure.
	 * 
	 * @param filterBy
	 *            Array of objects to filter the query by
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DatabaseObject> filter(Object... filterBy) {
		return (List<DatabaseObject>) manager.executeCursorProcedure("filter", filterBy).toListOf(this.getClass());
	}

	/**
	 * This is a implementation for using generics if you need a specific
	 * Database Object sub class. Searches for all DatabaseField annotations and
	 * calls the getters in order as an array of objects to be passed to the
	 * delete procedure.
	 * 
	 * @param type
	 *            Sub class to cast to
	 * @param filterBy
	 *            The objects to filter the result set with
	 * @return
	 */
	public <T extends DatabaseObject> List<T> filter(Class<T> type, Object... filterBy) {
		return manager.executeCursorProcedure("filter", filterBy).toListOf(type);
	}

	/**
	 * Default implementation for a updateable database object. Searches for all
	 * DatabaseField annotations and calls the getters in order as an array of
	 * objects to be passed to the delete procedure.
	 * 
	 * @return
	 */
	public boolean update() {
		Object[] params = getParameters(DatabaseFieldType.UPDATE);
		for(Object p : params) {
			if(p == null)
				return false;
		}
		return manager.executeStoredProcedure("update" + this.getClass().getSimpleName().replace("View", ""), params);
	}

	/**
	 * Default implementation for a deleteable database object. Searches for all
	 * DatabaseField annotations and calls the getters in order as an array of
	 * objects to be passed to the delete procedure.
	 * 
	 * @return
	 */
	public boolean delete() {
		return manager.executeStoredProcedure("delete", primaryKey);
	}

	/**
	 * This is the default toString
	 */
	@Override
	public String toString() {
		return "DatabaseObject [primaryKey=" + primaryKey + ", primaryKeyName=" + primaryKeyName + "]";
	}
}
