package br.ce.wcaquino.servicos;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CalculadoraMockTest {
	
	@Mock
	private Calculadora calcMock;
	
	@Spy
	private Calculadora calcSpy;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}
	
	
	@Test
	public void devoMostrarDiferencaEntreMockESpy() {
		//Retorna o valor padrão do mock caso a espectativa nao concretize
		Mockito.when(calcMock.somar(1, 5)).thenReturn(5);
		
		//Retorna o valor real do metodo caso a espectativa nao concretize
		//Mockito.when(calcMock.somar(1, 2)).thenCallRealMethod();
		
		//Spy retorna valor real do metodo caso expectativa nao concretize, mas nao funciona com interfaces
		//Mockito.when(calcSpy.somar(1, 2)).thenReturn(8);
		
		//Para evitar execução do método no Spy
		Mockito.doReturn(5).when(calcSpy).somar(1, 5);
		
		//Spy, por padrão, executa metodos void, para bloquear esse comportamento:
		Mockito.doNothing().when(calcSpy).imprime();
		
		
		System.out.println("Mock:" + calcMock.somar(1, 5));
		System.out.println("Spy:" + calcSpy.somar(1, 5));
		
		System.out.println("Mock:");
		calcMock.imprime();
		
		System.out.println("Spy:");
		calcSpy.imprime();
	}
	
	
	
	@Test
	public void teste() {
		Calculadora calc = Mockito.mock(Calculadora.class);
		
		ArgumentCaptor<Integer> argCapt = ArgumentCaptor.forClass(Integer.class);
		Mockito.when(calc.somar(argCapt.capture(), argCapt.capture())).thenReturn(5);
		
		Assert.assertEquals(5, calc.somar(1, 1156));
		//System.out.println(argCapt.getAllValues());
	}

}
