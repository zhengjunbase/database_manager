package com.github.mlaursen.database.objects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.mlaursen.annotations.DatabaseField;
import com.github.mlaursen.annotations.DatabaseFieldType;

/**
 * Basic outline for a DatbaseObject. Every database object must have at least a
 * primary key
 * 
 * @author mikkel.laursen
 * 
 */
public abstract class DatabaseObject {

	@DatabaseField(values = { DatabaseFieldType.GET, DatabaseFieldType.DELETE, DatabaseFieldType.UPDATE })
	protected String primaryKey;
	protected String primaryKeyName = "id";

	public DatabaseObject() { }
	public DatabaseObject(String primaryKey) {
		this.primaryKey = primaryKey;
	}
	
	/**
	 * Sets the primary key to the database column described as the
	 * primaryKeyName. The default is 'id'
	 * 
	 * @param r
	 */
	public DatabaseObject(MyResultRow r) {
		if(r.get(primaryKeyName) != null)
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
			if (m.getName().startsWith("set") && Arrays.asList(m.getParameterTypes()).contains(MyResultRow.class) && r != null) {
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
	
	
	public List<Procedure> getCustomProcedures() {
		return new ArrayList<Procedure>();
	}
	
	/**
	 * This is a basic check for if a Database object equals another.
	 * It just checks if the primary Key values are equal.
	 * @param o The object to compare to
	 */
	@Override
	public boolean equals(Object o) {
		return (o instanceof DatabaseObject) && primaryKey.equals(((DatabaseObject) o).primaryKey);
	}

	/**
	 * This is the default toString
	 */
	@Override
	public String toString() {
		return "DatabaseObject [primaryKey=" + primaryKey + ", primaryKeyName=" + primaryKeyName + "]";
	}
}
