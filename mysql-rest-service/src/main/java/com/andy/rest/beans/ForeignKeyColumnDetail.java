package com.andy.rest.beans;

public class ForeignKeyColumnDetail {

    private String name;
    private String foreignKeyTable;
    private String foreignKeyColumn;

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getForeignKeyTable() {
	return foreignKeyTable;
    }

    public void setForeignKeyTable(String foreignKeyTable) {
	this.foreignKeyTable = foreignKeyTable;
    }

    public String getForeignKeyColumn() {
	return foreignKeyColumn;
    }

    public void setForeignKeyColumn(String foreignKeyColumn) {
	this.foreignKeyColumn = foreignKeyColumn;
    }

    @Override
    public String toString() {
	return "ColumnDetail [name=" + name + ", foreignKeyTable=" + foreignKeyTable + ", foreignKeyColumn=" + foreignKeyColumn
		+ "]";
    }
}