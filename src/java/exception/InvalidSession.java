/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exception;

/**
 *
 * @author Vinz
 */
public class InvalidSession extends RuntimeException{

    public InvalidSession() {
    }

    /**
     * Constructs an instance of <code>InvalidSession</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public InvalidSession(String msg) {
        super(msg);
    }
}
