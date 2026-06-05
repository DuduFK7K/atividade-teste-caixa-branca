package login;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Classe que reproduz fielmente o código-fonte original fornecido para a atividade.
 * Contém vulnerabilidades estruturais, problemas de legibilidade, vazamento de conexões
 * e falhas de segurança propositais para fins de análise estática e teste de caixa branca.
 */
public class User {
    
    // Método para conectar ao banco de dados MySQL
    public Connection conectarBD() {
        Connection conn = null;
        try {
            // PROBLEMA 1: Uso incorreto do nome da classe do driver e chamada de newInstance() obsoleta
            Class.forName("com.mysql.Driver.Manager").newInstance();
            
            // PROBLEMA 2: Credenciais do banco expostas diretamente na URL (hardcoded)
            String url = "jdbc:mysql://127.0.0.1/test?user=lopes&password=123";
            conn = DriverManager.getConnection(url);
        } catch (Exception e) { 
            // PROBLEMA 3: Bloco catch vazio (silencia exceções e dificulta a depuração)
        }
        return conn;
    }
    
    // PROBLEMA 4: Variáveis de instância públicas que guardam estado da última consulta
    // Isso causa problemas graves de concorrência/thread-safety em aplicações web
    public String nome = "";
    public boolean result = false;
    
    // Método para verificar as credenciais do usuário
    public boolean verificarUsuario(String login, String senha) {
        String sql = "";
        Connection conn = conectarBD();
        
        // INSTRUÇÃO SQL
        // PROBLEMA 5: Vulnerabilidade grave a SQL Injection através da concatenação direta de parâmetros
        sql += "select nome from usuarios ";
        sql += "where login = " + "'" + login + "'";
        sql += " and senha = " + "'" + senha + "';";
        
        try {
            // PROBLEMA 6: Risco altíssimo de NullPointerException se conn for null
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            
            if (rs.next()) {
                result = true;
                nome = rs.getString("nome");
            }
            // PROBLEMA 7: Sem fechamento de recursos (Statement, ResultSet, Connection) - Resource Leak
        } catch (Exception e) {
            // PROBLEMA 8: Bloco catch vazio silenciando quaisquer exceções de banco de dados
        }
        
        return result;
    }
} // fim da class
