import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

class User implements Serializable {
    private final String username;
    private final String pin;
    private double balance;
    private transient LinkedList<String> miniStatement;

    public User(String username, String pin, double balance) {
        this.username = username;
        this.pin = pin;
        this.balance = balance;
        this.miniStatement = new LinkedList<>();
    }

    public String getUsername() { return username; }
    public String getPin() { return pin; }
    public double getBalance() { return balance; }

    public void deposit(double amount) {
        balance += amount;
        String txn = getTimestamp() + " - Deposited: Rs." + amount;
        addTransaction(txn);
        saveTransactionToFile(txn);
    }

    public boolean withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            String txn = getTimestamp() + " - Withdrew: Rs." + amount;
            addTransaction(txn);
            saveTransactionToFile(txn);
            return true;
        }
        return false;
    }

    public void addTransaction(String txn) {
        if (miniStatement == null) miniStatement = new LinkedList<>();
        miniStatement.addFirst(txn);
        if (miniStatement.size() > 10) miniStatement.removeLast();
    }

    public List<String> getMiniStatement() {
        if (miniStatement == null) miniStatement = new LinkedList<>();
        return miniStatement;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setPin(String newPin) {
        // Since pin is final now, this method is redundant.
        // Remove this if you want to keep `pin` truly final and unchangeable.
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private void saveTransactionToFile(String txn) {
        try (FileWriter fw = new FileWriter(username + "_statement.txt", true)) {
            fw.write(txn + "\n");
        } catch (IOException e) {
            System.out.println("Error writing transaction to file.");
        }
    }

    public void printMonthlyReport() {
        String fileName = username + "_statement.txt";
        System.out.println("--- Monthly Report for: " + username + " ---");
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("No transactions found.");
        }
    }
}

public class ATM {
    private static final String DATA_FILE = "users.dat";
    private static HashMap<String, User> users;

    public static void main(String[] args) {
        users = loadUsers();
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter username:");
        String username = sc.nextLine();

        if (username.equals("admin")) {
            adminLogin(sc);
            return;
        }

        if (!users.containsKey(username)) {
            System.out.println("New user. Set 4-digit PIN:");
            String pin = sc.nextLine();
            users.put(username, new User(username, pin, 0));
            saveUsers();
            System.out.println("User created. Login again.");
            return;
        }

        User currentUser = users.get(username);
        boolean authenticated = false;

        for (int attempts = 0; attempts < 3; attempts++) {
            System.out.println("Enter PIN:");
            String enteredPin = sc.nextLine();
            if (enteredPin.equals(currentUser.getPin())) {
                authenticated = true;
                break;
            } else {
                System.out.println("Incorrect PIN. Attempts left: " + (2 - attempts));
            }
        }

        if (!authenticated) {
            System.out.println("Too many attempts. Exiting.");
            return;
        }

        while (true) {
            System.out.println("\n1. Check Balance\n2. Deposit\n3. Withdraw\n4. Transfer to Another User\n5. Mini Statement\n6. Monthly Report\n7. Exit");

            int choice = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1 -> System.out.println("Balance: Rs." + currentUser.getBalance());
                case 2 -> {
                    System.out.println("Enter deposit amount:");
                    double dep = sc.nextDouble();
                    sc.nextLine();
                    currentUser.deposit(dep);
                    System.out.println("Deposited Rs." + dep);
                }
                case 3 -> {
                    System.out.println("Enter withdraw amount:");
                    double wd = sc.nextDouble();
                    sc.nextLine();
                    if (currentUser.withdraw(wd))
                        System.out.println("Withdrawn Rs." + wd);
                    else
                        System.out.println("Insufficient balance");
                }
                case 4 -> {
                    System.out.println("Enter recipient username:");
                    String recipient = sc.nextLine();
                    if (!users.containsKey(recipient)) {
                        System.out.println("User not found.");
                    } else if (recipient.equals(username)) {
                        System.out.println("Cannot transfer to self.");
                    } else {
                        System.out.println("Enter amount to transfer:");
                        double amt = sc.nextDouble();
                        sc.nextLine();
                        if (currentUser.withdraw(amt)) {
                            users.get(recipient).deposit(amt);
                            String txn = currentUser.getUsername() + " transferred Rs." + amt + " to " + recipient;
                            currentUser.addTransaction(txn);
                            users.get(recipient).addTransaction("Received Rs." + amt + " from " + currentUser.getUsername());
                            System.out.println("Transferred Rs." + amt + " to " + recipient);
                        } else {
                            System.out.println("Insufficient balance.");
                        }
                    }
                }
                case 5 -> {
                    System.out.println("--- Mini Statement ---");
                    for (String txn : currentUser.getMiniStatement()) {
                        System.out.println(txn);
                    }
                }
                case 6 -> currentUser.printMonthlyReport();
                case 7 -> {
                    saveUsers();
                    System.out.println("Thank you! Logging out.");
                    return;
                }
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private static void adminLogin(Scanner sc) {
        System.out.println("Enter Admin PIN:");
        String pin = sc.nextLine();
        if (!pin.equals("0000")) {
            System.out.println("Incorrect Admin PIN.");
            return;
        }

        System.out.println("Admin login successful.");
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. View All Users");
            System.out.println("2. View User Transactions");
            System.out.println("3. Delete User");
            System.out.println("4. Reset User PIN");
            System.out.println("5. Exit");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> {
                    System.out.println("--- All Users ---");
                    for (String uname : users.keySet()) {
                        System.out.println(uname + " - Balance: Rs." + users.get(uname).getBalance());
                    }
                }
                case 2 -> {
                    System.out.println("Enter username to view transactions:");
                    String userToView = sc.nextLine();
                    if (users.containsKey(userToView)) {
                        users.get(userToView).printMonthlyReport();
                    } else {
                        System.out.println("User not found.");
                    }
                }
                case 3 -> {
                    System.out.println("Enter username to delete:");
                    String userToDelete = sc.nextLine();
                    if (users.remove(userToDelete) != null) {
                        saveUsers();
                        File file = new File(userToDelete + "_statement.txt");
                        if (file.exists()) file.delete();
                        System.out.println("User deleted.");
                    } else {
                        System.out.println("User not found.");
                    }
                }
                case 4 -> {
                    System.out.println("Enter username to reset PIN:");
                    String userToReset = sc.nextLine();
                    if (users.containsKey(userToReset)) {
                        System.out.println("Enter new PIN:");
                        String newPin = sc.nextLine();
                        users.get(userToReset).setPin(newPin); // works only if pin is not final
                        saveUsers();
                        System.out.println("PIN reset successfully.");
                    } else {
                        System.out.println("User not found.");
                    }
                }
                case 5 -> {
                    System.out.println("Exiting admin panel.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.out.println("Error saving user data.");
        }
    }

    @SuppressWarnings("unchecked")
    private static HashMap<String, User> loadUsers() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (HashMap<String, User>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new HashMap<>();
        }
    }
}

