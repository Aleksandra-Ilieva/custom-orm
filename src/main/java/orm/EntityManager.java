package orm;

import annotations.Column;
import annotations.Entity;
import annotations.Id;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.LinkOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityManager<E> implements DbContext<E> {
    // if dataBase has id -> update else -> insert

    private Connection connection;

    public EntityManager(Connection connection) {
        this.connection = connection;
    }


    // this method returns Field id and if  it does not exist throws exception
    private Field getId(E entity) {
        Field[] fields = entity.getClass().getDeclaredFields(); // get all fields from given entity(Users,Students....)

        return Arrays.stream(entity.getClass().getDeclaredFields()).filter(x -> x.isAnnotationPresent(Id.class))
                .findFirst().orElseThrow(()-> new UnsupportedOperationException("Entity does not have primary key"));



    }

    @Override
    public boolean persist(E entity) throws IllegalAccessException, SQLException {
        Field primaryKey = getId(entity);
        primaryKey.setAccessible(true);

        Object value = primaryKey.get(entity);// return id value
        if (value == null || (long) value <= 0) {
            return doInsert(entity, primaryKey);
        }








        return doUpdate(entity, primaryKey);
    }

    private boolean doUpdate(E entity, Field primaryKey) throws IllegalAccessException, SQLException {
        Class<?> aClass = entity.getClass();
        String tableName = getTableName(aClass);


        String needIdForUpdating = primaryKey.get(entity).toString();
        String setClause = getFieldsAndValues(entity, primaryKey);

        String whereClause = String.format("id=%s", needIdForUpdating);


        String sqlQuery = String.format("UPDATE %s SET %s WHERE %s", tableName, setClause, whereClause);

        return connection.prepareStatement(sqlQuery).execute();


    }

    private String getFieldsAndValues(E entity, Field primaryKey) throws IllegalAccessException {
        String result = "";
        List<Field> fields = Arrays.stream(entity.getClass().getDeclaredFields()).filter(field -> !field.isAnnotationPresent(Id.class)).toList();


        for (Field field : fields) {
            field.setAccessible(true);

            if (field.get(entity) instanceof String || field.get(entity) instanceof LocalDate) {
                result += field.getAnnotation(Column.class).name() + "= '" + field.get(entity).toString() + "', ";
            }else {
                result += field.getAnnotation(Column.class).name() + "=" + field.get(entity).toString() + ", ";
            }






        }


        String substring = result.substring(0, result.length() - 2);
        return substring;
    }


    private boolean doInsert(E entity, Field primary) throws IllegalAccessException, SQLException {
        Class<?> aClass = entity.getClass();;
        String tableName = getTableName(aClass);
        String fieldsWithoutId = getFieldsWithoutId(entity);
        String fieldsValuesWithoutId = getFieldValuesWithoutId(entity);

        String query = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, fieldsWithoutId, fieldsValuesWithoutId);


        return connection.prepareStatement(query).execute();

    }


    private String getFieldValuesWithoutId(E entity) throws IllegalAccessException {
        List<String> valueList = new ArrayList<>();
        Field[] fields = entity.getClass().getDeclaredFields();
        List<Field> fieldList = new ArrayList<>();
        for (Field field : fields) {

            if (!field.isAnnotationPresent(Id.class)) {
                fieldList.add(field);
            }
        }

        for (Field field1 : fieldList) {
            field1.setAccessible(true);

            Object value = field1.get(entity);

            if (value instanceof String || value instanceof LocalDate) {
                valueList.add("'" + value + "'");


            } else {
                valueList.add(value.toString());
            }


        }

        return String.join(",", valueList);


    }

    private String getFieldsWithoutId(E entity) { // get the names of all  fields, because they are needed columns for an insert query
        Field[] fields = entity.getClass().getDeclaredFields();
        List<Field> fieldList = new ArrayList<>();
        for (Field field : fields) {

            if (!field.isAnnotationPresent(Id.class)) {
                fieldList.add(field);
            }

        }


        return fieldList.stream().map(field -> field.getAnnotation(Column.class).name()).collect(Collectors.joining(","));


    }

//    private String getTableName(E entity) {
//        String tableName = "";
//
//        tableName = entity.getClass().getAnnotation(Entity.class).name();
//
//        return tableName;
//
//
//    }


    private String getTableName(Class<?> aClass) {
        Entity[] annotationsByType = aClass.getAnnotationsByType(Entity.class);

        if (annotationsByType.length == 0) {
            throw new UnsupportedOperationException("Class must be Entity");
        }

        return annotationsByType[0].name();
    }


    @Override
    public Iterable<E> find(Class<E> table) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        String tableName = getTableName(table);


        String sql = String.format("SELECT * FROM %s",
                tableName);

        ResultSet result= connection.prepareStatement(sql).executeQuery();

        List<E> resultEntityList = new ArrayList<>(); // all entities
        E entity = table.getDeclaredConstructor().newInstance();

        result.next();

        fillEntity(table,result,entity);
        resultEntityList.add(entity);

        while (result.next()) {
            E entity2 = table.getDeclaredConstructor().newInstance();

            fillEntity(table,result,entity2);
            resultEntityList.add(entity2);
        }


        return resultEntityList;
    }

    @Override
    public Iterable<E> find(Class<E> table, String where) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String tableName = getTableName(table);


        String sql = String.format("SELECT * FROM %s %s",
                tableName, where == null ? "" : "WHERE " + where);

       ResultSet result= connection.prepareStatement(sql).executeQuery();

       List<E> resultEntityList = new ArrayList<>(); // all entities
        E entity = table.getDeclaredConstructor().newInstance();

        result.next();

        fillEntity(table,result,entity);
        resultEntityList.add(entity);

        while (result.next()) {
            E entity2 = table.getDeclaredConstructor().newInstance();

           fillEntity(table,result,entity2);
            resultEntityList.add(entity2);
        }


        return resultEntityList;
    }

    @Override
    public E findFirst(Class<E> table) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Statement statement = connection.createStatement();
        String tableName = getTableName(table);


        String query=String.format("SELECT * FROM %s  LIMIT 1;",tableName);

        ResultSet resultSet = statement.executeQuery(query);
        E entity = table.getDeclaredConstructor().newInstance();

        resultSet.next();

        fillEntity(table,resultSet,entity);
        return entity;

    }

    @Override
    public E findFirst(Class<E> table, String where) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        Statement statement = connection.createStatement();
        String tableName = getTableName(table);


        String query=String.format("SELECT * FROM %s %s LIMIT 1;",tableName,where!=null?"WHERE "+where:"");

        ResultSet resultSet = statement.executeQuery(query);
        E entity = table.getDeclaredConstructor().newInstance();

        resultSet.next();

        fillEntity(table,resultSet,entity);
        return entity;
    }


    private void fillEntity(Class<E> table,ResultSet resultSet,E entity) throws SQLException, IllegalAccessException {
        for (Field declaredField : table.getDeclaredFields()) {
            declaredField.setAccessible(true);
            fillField(declaredField,entity,resultSet);
        }

    }

    private  void fillField(Field field,E entity,ResultSet resultSet) throws SQLException, IllegalAccessException {
        field.setAccessible(true);

        if(field.getType()==int.class || field.getType()==long.class){
            field.set(entity,resultSet.getInt(field.getName()));
        }else if(field.getType()==LocalDate.class){
            field.set(entity,LocalDate.parse(resultSet.getString(field.getName())));
        }else {
            field.set(entity,resultSet.getString(field.getName()));
        }

    }


}
