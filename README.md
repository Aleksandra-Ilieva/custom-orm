# custom-orm
Java custom ORM with several methods, works with existing database.
Тhe ORM contains the following methods:

1. persist
The method inserts or updates a java entity into a table, depending on whether the object exists or not.
If the record exists, the persist method will update the entity with the provided id.
If the record does not exist, the persist method will insert an entity into the table.

2. findFirst with where clause
The method will find the first record from the table, depending on the where clause. The return value is a java object;

3. findFirst without where clause
The method will return the first record from the table.

4. find with where clause
The method will return a collection of objects, depending on the where clause

5. find without where clause
The method will return all the records from the table.



