package main.model.user;

public abstract class User {
    private String username;
    private String email;
    private String role;

    public User(String username, String email, String role) {
        this.role = role;
        this.username = username;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public void setEmail(String email) {this.email = email;}
    public void setUsername(String username) {this.username = username;}
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
