package br.ce.wcaquino.servicos;



import static br.ce.wcaquino.builders.FilmeBuilder.umFilme;
import static br.ce.wcaquino.builders.FilmeBuilder.umFilmeSemEstoque;
import static br.ce.wcaquino.builders.LocacaoBuilder.umLocacao;
import static br.ce.wcaquino.builders.UsuarioBuilder.umUsuario;
import static br.ce.wcaquino.matchers.MatchersProprios.caiNumaSegunda;
import static br.ce.wcaquino.matchers.MatchersProprios.ehHoje;
import static br.ce.wcaquino.matchers.MatchersProprios.ehHojeComDiferencaDias;
import static br.ce.wcaquino.utils.DataUtils.obterDataComDiferencaDias;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import br.ce.wcaquino.builders.LocacaoBuilder;
import br.ce.wcaquino.builders.UsuarioBuilder;
import br.ce.wcaquino.daos.LocacaoDAO;
import br.ce.wcaquino.daos.LocacaoDAOFake;
import br.ce.wcaquino.daos.SPCService;
import br.ce.wcaquino.entidades.Filme;
import br.ce.wcaquino.entidades.Locacao;
import br.ce.wcaquino.entidades.Usuario;
import br.ce.wcaquino.exceptions.FilmeSemEstoqueException;
import br.ce.wcaquino.exceptions.LocadoraException;
import br.ce.wcaquino.utils.DataUtils;
import buildermaster.BuilderMaster;


public class LocacaoServiceTest {

	@InjectMocks @Spy
	private LocacaoService service;
	
	@Mock
	private SPCService spc;
	@Mock
	private LocacaoDAO dao;
	@Mock
	private EmailService emailService;
	
	@Rule
	public ErrorCollector error = new ErrorCollector();
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setup(){
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void deveAlugarFilme() throws Exception {
		//Assume.assumeFalse(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));
		
		//cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().comValor(5.0).agora());
		
		Mockito.doReturn(DataUtils.obterData(28, 4, 2017)).when(service).obterData();
		
		//acao
		Locacao locacao = service.alugarFilme(usuario, filmes);
			
		//verificacao
		error.checkThat(locacao.getValor(), is(equalTo(5.0)));
		error.checkThat(DataUtils.isMesmaData(locacao.getDataLocacao(), DataUtils.obterData(28, 4, 2017)), is(true));
		error.checkThat(DataUtils.isMesmaData(locacao.getDataRetorno(), DataUtils.obterData(29, 4, 2017)), is(true));
	}
	
	@Test(expected = FilmeSemEstoqueException.class)
	public void naoDeveAlugarFilmeSemEstoque() throws Exception{
		//cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilmeSemEstoque().agora());
		
		//acao
		service.alugarFilme(usuario, filmes);
	}
	
	@Test
	public void naoDeveAlugarFilmeSemUsuario() throws FilmeSemEstoqueException{
		//cenario
		List<Filme> filmes = Arrays.asList(umFilme().agora());
		
		//acao
		try {
			service.alugarFilme(null, filmes);
			Assert.fail();
		} catch (LocadoraException e) {
			assertThat(e.getMessage(), is("Usuario vazio"));
		}
	}

	@Test
	public void naoDeveAlugarFilmeSemFilme() throws FilmeSemEstoqueException, LocadoraException{
		//cenario
		Usuario usuario = umUsuario().agora();
		
		exception.expect(LocadoraException.class);
		exception.expectMessage("Filme vazio");
		
		//acao
		service.alugarFilme(usuario, null);
	}
	
	@Test
	public void deveDevolverNaSegundaAoAlugarNoSabado() throws Exception{
		//Assume.assumeTrue(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));
		
		//cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());
		
		Mockito.doReturn(DataUtils.obterData(29, 4, 2017)).when(service).obterData();
		
		//acao
		Locacao retorno = service.alugarFilme(usuario, filmes);
		
		//verificacao
		assertThat(retorno.getDataRetorno(), caiNumaSegunda());
	}
	
	@Test
	public void naoDeveAlugarFilmeParaNegativadoSPC() throws Exception {
		//cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());
		
		when(spc.possuiNegativacao(Mockito.any(Usuario.class))).thenReturn(true);
		
		//acao
		try {
			service.alugarFilme(usuario, filmes);
		//verificacao
			Assert.fail();
		} catch (LocadoraException e) {
			assertThat(e.getMessage(), is("Usuário Negativado"));
		}
		
		Mockito.verify(spc).possuiNegativacao(usuario);
	}
	
	@Test
	public void deveEnviarEmailParaLocacoesAtrasadas() {
		//cenario
		Usuario usuario = umUsuario().agora();
		Usuario usuario2 = umUsuario().comNome("Usuario em dia").agora();
		Usuario usuario3 = umUsuario().comNome("Outro usuario").agora();
		List<Locacao> locacoes = Arrays.asList(
				umLocacao().atrasada().comUsuario(usuario).agora(),
				umLocacao().comUsuario(usuario2).agora(),
				umLocacao().atrasada().comUsuario(usuario3).agora(),
				umLocacao().atrasada().comUsuario(usuario3).agora());
		
		when(dao.obterLocacoesPendentes()).thenReturn(locacoes);
		
		//acao
		service.notificarAtrasos();
		
		//verificacao
		Mockito.verify(emailService, Mockito.times(3)).notificarAtraso(Mockito.any(Usuario.class));
		Mockito.verify(emailService).notificarAtraso(usuario);
		Mockito.verify(emailService, Mockito.atLeastOnce()).notificarAtraso(usuario3);
		Mockito.verify(emailService, Mockito.never()).notificarAtraso(usuario2);
		Mockito.verifyNoMoreInteractions(emailService);
	}
	
	@Test
	public void deveTratarErroNoSpc() throws Exception {
		//cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());
		
		
		when(spc.possuiNegativacao(usuario)).thenThrow(new Exception("Falha no serviço"));
		
		//verificacao
		exception.expect(LocadoraException.class);
		exception.expectMessage("Problemas no sistema do SPC, tente novamente");
		
		
		//acao
		service.alugarFilme(usuario, filmes);
	}
	
	@Test
	public void deveProrrogarUmaLocacao() {
		//cenario
		Locacao locacao = umLocacao().agora();
		
		//acao
		service.prorrogarLocacao(locacao, 3);
		
		//verificacao
		ArgumentCaptor<Locacao> argCapt = ArgumentCaptor.forClass(Locacao.class);
		Mockito.verify(dao).salvar(argCapt.capture());
		Locacao locacaoRetornada = argCapt.getValue();
		
		Assert.assertThat(locacaoRetornada.getValor(), is(12.0));
		Assert.assertThat(locacaoRetornada.getDataLocacao(), ehHoje());
		Assert.assertThat(locacaoRetornada.getDataRetorno(), ehHojeComDiferencaDias(3));
	}
		
	
	@Test
	public void deveCalcularValorLocacao() throws Exception {
		//cenario
		List<Filme> filmes = Arrays.asList(umFilme().agora());
		
		//acao
		Class<LocacaoService> clazz = LocacaoService.class;
		Method metodo = clazz.getDeclaredMethod("calcularValorLocacao", List.class);
		metodo.setAccessible(true);
		Double valor = (Double) metodo.invoke(service, filmes);
		
		
		//verificacao
		Assert.assertThat(valor, is(4.0));
	}
}
