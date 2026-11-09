/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model.exception;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Usuario o contraseña incorrectos");
    }
}
