import annotations.Entity;
import entities.User;
import orm.EntityManager;
import orm.MyConnector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        Scanner scanner = new Scanner(System.in);
        String password = scanner.nextLine();
        

        MyConnector.createConnection("root", password, "custom-orm");
        Connection connection = MyConnector.getConnection();

        EntityManager<User> userEntityManager = new EntityManager<>(connection);


        User user2 = new User("Pesho", 3);
        userEntityManager.persist(user2); // insert into table

        User user = new User("Pesho", 6);
        user.setId(9);
        userEntityManager.persist(user); //update







        User users = userEntityManager.findFirst(User.class, "age=19"); //find first with where clause
        System.out.println(users.getUsername());

       User firstUser = userEntityManager.findFirst(User.class); // find  first user from table
        System.out.println(firstUser.getUsername());



        userEntityManager  // find collection with users with where clause from table
                .find(User.class, "age >= 19")
                .forEach(u -> System.out.println(u.getUsername()));


        System.out.println("find"); // find all records from table
        userEntityManager
                .find(User.class)
                .forEach(u -> System.out.println(u.getUsername()));
    }
}
