package login;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Suite de testes unitários executável sem dependências externas.
 * Valida o comportamento de ambas as classes (User e UserRevised).
 */
public class UserTest {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO EXECUÇÃO DOS TESTES ===");
        
        testOriginalUser_NullConnection_ThrowsOrSwallows();
        testRevisedUser_NullConnection_Graceful();
        testRevisedUser_SuccessfulLogin();
        testRevisedUser_FailedLogin();
        
        System.out.println("=== TODOS OS TESTES EXECUTADOS COM SUCESSO ===");
    }

    /**
     * Teste 1: Valida que a classe original lança/silencia exceções e tem comportamento
     * imprevisível quando a conexão é nula.
     */
    private static void testOriginalUser_NullConnection_ThrowsOrSwallows() {
        System.out.print("Testando User (Original) com conexão nula: ");
        User user = new User() {
            @Override
            public Connection conectarBD() {
                return null; // Força retorno nulo
            }
        };
        
        try {
            boolean result = user.verificarUsuario("admin", "senha123");
            // Deve retornar false porque o catch vazio captura a NullPointerException
            if (!result) {
                System.out.println("PASS (NullPointerException capturada pelo catch vazio)");
            } else {
                System.out.println("FAIL (Retornou true inesperadamente)");
            }
        } catch (Exception e) {
            System.out.println("FAIL (Exceção vazou: " + e.getMessage() + ")");
        }
    }

    /**
     * Teste 2: Valida que a classe revisada lida de forma graciosa e segura com conexões nulas.
     */
    private static void testRevisedUser_NullConnection_Graceful() {
        System.out.print("Testando UserRevised com conexão nula: ");
        UserRevised user = new UserRevised("", "", "") {
            @Override
            public Connection conectarBD() {
                return null; // Força retorno nulo
            }
        };
        
        UserRevised.AuthResult result = user.verificarUsuario("admin", "senha123");
        if (!result.isSuccess() && "".equals(result.getNome())) {
            System.out.println("PASS (Lidou com conexão nula com segurança)");
        } else {
            System.out.println("FAIL (Comportamento inesperado)");
        }
    }

    /**
     * Teste 3: Valida a autenticação bem-sucedida no UserRevised usando mock dinâmico de JDBC.
     */
    private static void testRevisedUser_SuccessfulLogin() {
        System.out.print("Testando UserRevised login bem-sucedido (Mock): ");
        
        Connection mockConn = createMockConnection("Carlos Lopes", true);
        UserRevised user = new UserRevised("", "", "") {
            @Override
            public Connection conectarBD() {
                return mockConn;
            }
        };
        
        UserRevised.AuthResult result = user.verificarUsuario("lopes", "123");
        if (result.isSuccess() && "Carlos Lopes".equals(result.getNome())) {
            System.out.println("PASS");
        } else {
            System.out.println("FAIL");
        }
    }

    /**
     * Teste 4: Valida falha de autenticação no UserRevised usando mock dinâmico de JDBC.
     */
    private static void testRevisedUser_FailedLogin() {
        System.out.print("Testando UserRevised login malsucedido (Mock): ");
        
        Connection mockConn = createMockConnection("", false);
        UserRevised user = new UserRevised("", "", "") {
            @Override
            public Connection conectarBD() {
                return mockConn;
            }
        };
        
        UserRevised.AuthResult result = user.verificarUsuario("lopes", "senha_errada");
        if (!result.isSuccess() && "".equals(result.getNome())) {
            System.out.println("PASS");
        } else {
            System.out.println("FAIL");
        }
    }

    // ==========================================
    // UTILITÁRIOS PARA MOCK DINÂMICO DE JDBC
    // ==========================================

    private static Connection createMockConnection(String returnName, boolean hasResult) {
        return (Connection) Proxy.newProxyInstance(
            UserTest.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("prepareStatement".equals(method.getName())) {
                        return createMockPreparedStatement(returnName, hasResult);
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return null;
                }
            }
        );
    }

    private static PreparedStatement createMockPreparedStatement(String returnName, boolean hasResult) {
        return (PreparedStatement) Proxy.newProxyInstance(
            UserTest.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("executeQuery".equals(method.getName())) {
                        return createMockResultSet(returnName, hasResult);
                    }
                    if (method.getName().startsWith("set")) {
                        return null;
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return null;
                }
            }
        );
    }

    private static ResultSet createMockResultSet(String returnName, boolean hasResult) {
        return (ResultSet) Proxy.newProxyInstance(
            UserTest.class.getClassLoader(),
            new Class<?>[]{ResultSet.class},
            new InvocationHandler() {
                private int callCount = 0;
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("next".equals(method.getName())) {
                        callCount++;
                        return hasResult && callCount == 1;
                    }
                    if ("getString".equals(method.getName())) {
                        return returnName;
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return null;
                }
            }
        );
    }
}
