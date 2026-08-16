package br.com.gauderio;

import br.com.gauderio.dao.CategoriaDAO;
import br.com.gauderio.dao.ContaDAO;
import br.com.gauderio.dao.MovimentacaoDAO;
import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.db.Database;
import br.com.gauderio.model.Categoria;
import br.com.gauderio.model.ContaBancaria;
import br.com.gauderio.model.MovimentacaoSaldo;
import br.com.gauderio.model.Transacao;

import java.time.LocalDate;
import java.util.List;

/** Smoke test da camada de persistência SQLite (não faz parte da aplicação). */
public class SmokeTest {

    public static void main(String[] args) {
        Database.init();
        System.out.println("Banco criado em: " + Database.getDbPath());

        // Categorias semeadas
        List<Categoria> categorias = new CategoriaDAO().findAll();
        System.out.println("Categorias carregadas: " + categorias.size());

        // Conta
        ContaDAO contaDAO = new ContaDAO();
        int idConta = contaDAO.insert(new ContaBancaria("Conta Corrente Banrisul", "Banrisul", "0102", "12345-6", 1000.00));
        System.out.println("Conta inserida id=" + idConta);

        // Movimentação de saldo
        MovimentacaoDAO movDAO = new MovimentacaoDAO();
        movDAO.insert(new MovimentacaoSaldo(idConta, MovimentacaoSaldo.ENTRADA, "Depósito inicial extra", 500.00, LocalDate.now()));
        System.out.println("Movimentação registrada.");

        // Lançamentos: receita paga, despesa paga, boleto futuro
        TransacaoDAO tdao = new TransacaoDAO();
        tdao.insert(new Transacao(Transacao.TIPO_RECEITA, "Venda de produtos", 2500.00, "Vendas",
                "Conta Corrente Banrisul", LocalDate.now(), null, Transacao.STATUS_PAGO, false, null));
        tdao.insert(new Transacao(Transacao.TIPO_DESPESA, "Energia elétrica", 320.10, "Energia elétrica",
                "Conta Corrente Banrisul", LocalDate.now(), null, Transacao.STATUS_PAGO, false, null));
        tdao.insert(new Transacao(Transacao.TIPO_DESPESA, "Parcela do financiamento", 890.00, "Impostos",
                "Conta Corrente Banrisul", LocalDate.now(), LocalDate.now().plusDays(15), Transacao.STATUS_PENDENTE, true, "MENSAL"));

        // Consultas e totais
        System.out.println("Total transações: " + tdao.findAll().size());
        System.out.println("Pendentes: " + tdao.pendentes().size());
        System.out.println("Soma pendente DESPESA: " + tdao.somaPendente(Transacao.TIPO_DESPESA));
        System.out.println("Saldo da conta: " + contaDAO.saldoAtual("Conta Corrente Banrisul"));

        System.out.println("SMOKE OK");
        System.exit(0);
    }
}