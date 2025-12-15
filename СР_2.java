abstract class Payment {
    private double amount; // інкапсуляція

    protected Payment(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public abstract boolean process(); 
}

class CardPayment extends Payment {

    public CardPayment(double amount) {
        super(amount);
    }

    @Override
    public boolean process() {
        System.out.println("Processing card payment: " + getAmount());
        return true;
    }
}

class CryptoPayment extends Payment {

    public CryptoPayment(double amount) {
        super(amount);
    }

    @Override
    public boolean process() {
        System.out.println("Processing crypto payment: " + getAmount());
        return true;
    }
}



interface PaymentValidator {
    boolean validate(Payment payment);
}

class DefaultPaymentValidator implements PaymentValidator {

    @Override
    public boolean validate(Payment payment) {
        return payment.getAmount() > 0;
    }
}



class PaymentService {


    private static class Logger {
        static void log(String msg) {
            System.out.println("[LOG] " + msg);
        }
    }

    private final PaymentValidator validator;

    public PaymentService(PaymentValidator validator) {
        this.validator = validator;
    }

    public boolean execute(Payment payment) {
        Logger.log("Start payment. Amount=" + payment.getAmount());

        if (!validator.validate(payment)) {
            Logger.log("Validation failed");
            return false;
        }

        boolean result = payment.process();
        Logger.log("Payment result=" + result);
        return result;
    }
}


class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();

    private ConfigManager() {}

    public static ConfigManager getInstance() {
        return INSTANCE;
    }


    public String environment() {
        return "DEV";
    }
}


class PaymentFactory {

    public static Payment createPayment(String type, double amount) {
        return switch (type) {
            case "CARD" -> new CardPayment(amount);
            case "CRYPTO" -> new CryptoPayment(amount);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}

public class PaymentSystem {

    public static void main(String[] args) {

        ConfigManager cfg = ConfigManager.getInstance();
        System.out.println("Environment: " + cfg.environment());


        PaymentValidator defaultValidator = new DefaultPaymentValidator();

        PaymentValidator maxLimitValidator = new PaymentValidator() {
            @Override
            public boolean validate(Payment payment) {
                return payment.getAmount() > 0 && payment.getAmount() < 10_000;
            }
        };

        PaymentService service1 = new PaymentService(defaultValidator);
        PaymentService service2 = new PaymentService(maxLimitValidator);

        // Factory Method
        List<Payment> payments = List.of(
                PaymentFactory.createPayment("CARD", 100),
                PaymentFactory.createPayment("CRYPTO", 500),
                PaymentFactory.createPayment("CARD", -10),   
                PaymentFactory.createPayment("CRYPTO", 15000)  
        );

        System.out.println("\n--- Execute with defaultValidator ---");
        service1.execute(payments.get(0));
        service1.execute(payments.get(2));

        System.out.println("\n--- Execute with maxLimitValidator ---");
        service2.execute(payments.get(1));
        service2.execute(payments.get(3));
    }
}


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentFactoryTest {

    @Test
    void testFactoryCreatesCardPayment() {
        Payment payment = PaymentFactory.createPayment("CARD", 100);
        assertTrue(payment instanceof CardPayment);
        assertTrue(payment.process());
    }

    @Test
    void testFactoryCreatesCryptoPayment() {
        Payment payment = PaymentFactory.createPayment("CRYPTO", 200);
        assertTrue(payment instanceof CryptoPayment);
        assertTrue(payment.process());
    }

    @Test
    void testFactoryThrowsOnUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> PaymentFactory.createPayment("BANK", 100));
    }
}

class SingletonTest {

    @Test
    void testSingletonSameInstance() {
        ConfigManager a = ConfigManager.getInstance();
        ConfigManager b = ConfigManager.getInstance();
        assertSame(a, b);
    }
}

class PaymentServiceTest {

    @Test
    void testExecuteSuccess() {
        PaymentValidator validator = new DefaultPaymentValidator();
        PaymentService service = new PaymentService(validator);

        Payment payment = new CardPayment(100);
        assertTrue(service.execute(payment));
    }

    @Test
    void testExecuteValidationFail() {
        PaymentValidator validator = new DefaultPaymentValidator();
        PaymentService service = new PaymentService(validator);

        Payment payment = new CardPayment(-1);
        assertFalse(service.execute(payment));
    }

    @Test
    void testExecuteWithAnonymousValidatorLimit() {
        PaymentValidator limitValidator = new PaymentValidator() {
            @Override
            public boolean validate(Payment payment) {
                return payment.getAmount() > 0 && payment.getAmount() < 10_000;
            }
        };

        PaymentService service = new PaymentService(limitValidator);

        assertTrue(service.execute(new CryptoPayment(9999)));
        assertFalse(service.execute(new CryptoPayment(10000)));
    }
}
