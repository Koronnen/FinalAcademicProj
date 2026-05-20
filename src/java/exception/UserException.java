/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exception;

/**
 *
 * @author Marjorie
 */
public class UserException extends RuntimeException{ //signup

    public UserException() {
    }

    /**
     * Constructs an instance of <code>InvalidSession</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public UserException(String msg) {
        super(msg);
    }
}
