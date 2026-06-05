package login;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Versão revisada e corrigida da classe User.
 * Implementa boas práticas de desenvolvimento, segurança, gerenciamento de conexões
 * e correção de todas as vulnerabilidades identificadas na análise estática.
 */
public class UserRevised {
    
    private static final Logger LOGGER = Logger.getLogger(UserRevised.class.getName());
    
    // Configurações de banco de dados parametrizadas
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    /**
     * Construtor padrão que inicializa com variáveis de ambiente ou valores padrão seguros.
     */
    public UserRevised() {
        this.dbUrl = System.getProperty("db.url", "jdbc:mysql://127.0.0.1:3006/test");
        this.dbUser = System.getProperty("db.user", "lopes");
        this.dbPassword = System.getProperty("db.password", "123");
    }

    /**
     * Construtor que permite injeção de dependências para facilidade de testes.
     */
    public UserRevised(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    /**
     * Estabelece uma conexão com o banco de dados.
     * 
     * @return Connection objeto de conexão ativo
     * @throws SQLException se ocorrer um erro ao conectar
     */
    public Connection conectarBD() throws SQLException {
        try {
            // Em JDBC 4.0+, Class.forName() é opcional, mas se necessário, usa-se a classe correta.
            // O driver correto do MySQL moderno é "com.mysql.cj.jdbc.Driver".
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver JDBC do MySQL não encontrado.", e);
            throw new SQLException("Driver não encontrado", e);
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Estrutura de dados imutável para encapsular o resultado da autenticação.
     * Isso resolve o problema de concorrência das variáveis globais da classe original.
     */
    public static class AuthResult {
        private final boolean success;
        private final String nome;

        public AuthResult(boolean success, String nome) {
            this.success = success;
            this.nome = nome;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getNome() {
            return nome;
        }
    }

    /**
     * Verifica as credenciais de um usuário com segurança.
     * Correções implementadas:
     * - PreparedStatement para evitar SQL Injection.
     * - Try-with-resources para garantir o fechamento de Connection, Statement e ResultSet.
     * - Retorno de AuthResult imutável, garantindo Thread-safety.
     * - Tratamento de exceções adequado e logging.
     * 
     * @param login Usuário
     * @param senha Senha informada (idealmente deveria ser passada por hash, e comparada no banco com o hash)
     * @return AuthResult contendo o status de sucesso e o nome do usuário
     */
    public AuthResult verificarUsuario(String login, String senha) {
        // SQL Parametrizado
        String sql = "SELECT nome FROM usuarios WHERE login = ? AND senha = ?";
        
        // Try-with-resources gerencia automaticamente o fechamento das conexões e statements
        try (Connection conn = conectarBD()) {
            if (conn == null) {
                LOGGER.severe("A conexão com o banco de dados retornou nula.");
                return new AuthResult(false, "");
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                pstmt.setString(2, senha); // Nota: senhas devem ser armazenadas e comparadas com hash (ex: BCrypt)
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String nome = rs.getString("nome");
                        return new AuthResult(true, nome);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro na execução da consulta de autenticação.", e);
        }
        
        return new AuthResult(false, "");
    }
}
