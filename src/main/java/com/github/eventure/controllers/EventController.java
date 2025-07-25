
package com.github.eventure.controllers;

import java.util.ArrayList;
//import java.time.LocalTime;
//import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import com.github.eventure.model.Address;
import com.github.eventure.model.Event;
import com.github.eventure.model.EventClassification;
import com.github.eventure.model.User;
import com.github.eventure.model.Visibilidade;
import com.github.eventure.model.address.Cep;
import com.github.eventure.storage.Storage;

public class EventController {
	private Storage<Event> eventController;
	private static EventController instance;
	private static int lastGeneratedId = 0;

	private EventController() {
		if (eventController == null) {
			eventController = new Storage<Event>();
		}
	}

	public static EventController getInstance() {
		if (instance == null) {
			instance = new EventController();
		}
		return instance;
	}
	
	public void desativarEventosDoUsuario(int userId) {
	    for (Event e : eventController) {
	        if (e.getIdMaker() == userId) {
	            e.setAtivo(false);
	        }
	    }
	}
	public void ativarEventosDoUsuario(int userId) {
	    for (Event e : eventController) {
	        if (e.getIdMaker() == userId) {
	            e.setAtivo(true);
	        }
	    }
	}

	public void createEvent(int idMaker, String title, String description, EventClassification type, String date,String dateEnd,
			String startHours, String endHours, String caminho, String cep, String estado, String cidade, String bairro,
			String rua, String numero, String complemento,Visibilidade visibilidade) {
		var e = new Event();
		var address = new Address();
		e.setTitle(title);
		e.setDescription(description);
		e.setType(type);
		e.setDate(date);
		e.setDateEnd(dateEnd);
		e.setStartHours(startHours);
		e.setEndHours(endHours);
		e.setImagePath(caminho);
		address.setCep(cep);
		address.setEstado(estado);
		address.setCidade(cidade);
		address.setBairro(bairro);
		address.setRua(rua);
		address.setNumero(numero);
		e.setVisibilidade(visibilidade);
		if (!complemento.isEmpty()) {
			address.setComplemento(complemento);
		}
		e.setAddress(address);
		if (!e.getTitle().isEmpty() && !e.getDescription().isEmpty() && !e.getDate().isEmpty() && !e.getStartHours().isEmpty()
				&& !e.getImagePath().isEmpty() && e.getAddress() != null) {
			e.setId(generateId());
			e.setIdMaker(idMaker);
			e.addConfirmedParticipantIds(idMaker);
			var userController = UserController.getInstance();
			var user = userController.findUserById(idMaker);
			user.addListMyEvents(e.getId());
			eventController.add(e);
			JOptionPane.showMessageDialog(null, "Evento criado com sucesso!");
		} else {
			JOptionPane.showMessageDialog(null, "Erro ao criar evento, Preencha os campos corretamente");
		}

	}

	public void createEvent(int id, String description, String title, EventClassification type) {

	}

	public void deleteEvent(Event e) {
		eventController.remove(e);
	}

	public void deleteEventById(int id) {
		var e = findEventById(id);
		int idMaker = e.getIdMaker();
		var userController = UserController.getInstance();
		User userMaker = userController.findUserById(idMaker);
		userMaker.removeListMyEvents(id);
		for(int i = 0; i < e.getConfirmedParticipantIds().size(); i++)
		{
			int idUser = e.getConfirmedParticipantIds().get(i); 
			var user = userController.findUserById(idUser);
			user.removeListaEventos(id);
		}
		if(e.getVisibilidade() == Visibilidade.PRIVADO)
		{
			e.getPendingRequestIds().clear();
		}
		e.getParticipacaoConfirmada().clear();
		eventController.remove(e);
	}

	public Event findEventById(int id) {
		return eventController.find(event -> event.getId() == id).findFirst().orElse(null);
	}

	public Event findEventByExactTitle(String title) {
		return eventController.find(event -> event.getTitle().equals(title)).findFirst().orElse(null);
	}

	public List<Event> findEventsByTitleContaining(String title) {
		return eventController.find(event -> event.getTitle().toLowerCase().contains(title.toLowerCase()))
				.collect(Collectors.toList());
	}
	
	public boolean haEventosPrivadosComRequisicoesPendentes(int id) {
		var userController =  UserController.getInstance();
		var user = userController.findUserById(id);
	    var eventosId = user.getMyEventsList();
	    List<Event> eventos = new ArrayList<>();
	    for(int i = 0; i < eventosId.size(); i++)
	    {
	    	eventos.add(findEventById(eventosId.get(i)));
	    }
	    int contador = 0;

	    for (Event e : eventos) {
	        if (e.getVisibilidade() == Visibilidade.PRIVADO) {
	            contador += e.getPendingRequestIds().size();
	        }
	    }
	    return contador > 0;
	}

	public void eventClone(int idEvent, String title, String description, EventClassification type, String date, String dateEnd,
			String startHours, String endHours, String caminho, String cep, String estado, String cidade, String bairro,
			String rua, String numero, String complemento,Visibilidade visibilidade) {
		var eventClone = new Event();
		eventClone.setTitle(title);
		eventClone.setDescription(description);
		eventClone.setType(type);
		eventClone.setDate(date);
		eventClone.setDateEnd(dateEnd);
		eventClone.setStartHours(startHours);
		eventClone.setEndHours(endHours);
		eventClone.setImagePath(caminho);
		var address = new Address();
		address.setCep(cep);
		address.setEstado(estado);
		address.setCidade(cidade);
		address.setBairro(bairro);
		address.setComplemento(complemento);
		address.setNumero(numero);
		address.setRua(rua);
		eventClone.setAddress(address);
		eventClone.setVisibilidade(visibilidade);
		eventClone.setId(idEvent);
		applyChanges(idEvent, eventClone);
	}

	public void applyChanges(int id, Event eventClone) {
		var event = findEventById(id);
		if (event == null) {
			return;
		}
		if (eventClone.getTitle() != null && !eventClone.getTitle().trim().isEmpty()
				&& !eventClone.getTitle().equals(event.getTitle())) {
			event.setTitle(eventClone.getTitle());
		}

		if (eventClone.getDescription() != null && !eventClone.getDescription().trim().isEmpty()
				&& !(eventClone.getDescription().equals(event.getDescription()))) {
			event.setDescription(eventClone.getDescription());
		}
		if (eventClone.getTitle() != null && !eventClone.getTitle().trim().isEmpty()
				&& !(eventClone.getTitle().equals(event.getTitle()))) {
			event.setTitle(eventClone.getTitle());
		}
		if (eventClone.getDate() != null && !(eventClone.getDate().equals(event.getDate()))) {
			event.setDate(eventClone.getDate());
		}
		if (eventClone.getDateEnd() != null && !(eventClone.getDateEnd().equals(event.getDateEnd()))) {
			event.setDateEnd(eventClone.getDateEnd());
		}
		if (eventClone.getStartHours() != null && !(eventClone.getStartHours().equals(event.getStartHours()))) {
			event.setStartHours(eventClone.getStartHours());
		}
		if (eventClone.getEndHours() != null && !(eventClone.getEndHours().equals(event.getEndHours()))) {
			event.setEndHours(eventClone.getEndHours());
		}
		if (eventClone.getImagePath() != null && !(eventClone.getImagePath().equals(event.getImagePath()))) {
			event.setImagePath(eventClone.getImagePath());
		}
		if (eventClone.getAddress().getCep() != null
				&& !(eventClone.getAddress().getCep().equals(event.getAddress().getCep()))) {
			event.getAddress().setCep(eventClone.getAddress().getCep());
		}
		if (eventClone.getAddress().getEstado() != null
				&& !(eventClone.getAddress().getEstado().equals(event.getAddress().getEstado()))) {
			event.getAddress().setEstado(eventClone.getAddress().getEstado());
		}
		if (eventClone.getAddress().getCidade() != null
				&& !(eventClone.getAddress().getCidade().equals(event.getAddress().getCidade()))) {
			event.getAddress().setCidade(eventClone.getAddress().getCidade());
		}
		if (eventClone.getAddress().getBairro() != null
				&& !(eventClone.getAddress().getBairro().equals(event.getAddress().getBairro()))) {
			event.getAddress().setBairro(eventClone.getAddress().getBairro());
		}
		if (eventClone.getAddress().getRua() != null
				&& !(eventClone.getAddress().getRua().equals(event.getAddress().getRua()))) {
			event.getAddress().setRua(eventClone.getAddress().getRua());
		}
		if (eventClone.getAddress().getComplemento() != null
				&& !(eventClone.getAddress().getComplemento().equals(event.getAddress().getComplemento()))) {
			event.getAddress().setComplemento(eventClone.getAddress().getComplemento());
		}
		if (eventClone.getAddress().getNumero() != null
				&& !(eventClone.getAddress().getNumero().equals(event.getAddress().getNumero()))) {
			event.getAddress().setNumero(eventClone.getAddress().getNumero());
		}
		if(eventClone.getVisibilidade() != null && eventClone.getVisibilidade() != event.getVisibilidade())
		{
			event.setVisibilidade(eventClone.getVisibilidade());
		}

	}
	public List<Integer> getConfirmadosQueNaoCompareceram(int eventId) {
	    Event evento = findEventById(eventId);
	    List<Integer> confirmados = evento.getConfirmedParticipantIds();
	    List<Integer> compareceram = evento.getParticipacaoConfirmada();

	    return confirmados.stream()
	            .filter(id -> !compareceram.contains(id))
	            .collect(Collectors.toList());
	}
	
	public void adicionarParticipante(int idEvent, int idUser)
	{
		UserController userController = UserController.getInstance();
		Event event = findEventById(idEvent);
		User user = userController.findUserById(idUser);
		if(!event.usersParticipaOuNão(idUser))
		{
		 if(event.getVisibilidade() == Visibilidade.PUBLICO)
		 {
			 user.addListaEventos(idEvent);
			event.addConfirmedParticipantIds(idUser);
			
			JOptionPane.showInternalMessageDialog(null,"Presença confirmada");
		 }else
		  {
			 if(event.usersParticipaOuNãoListPending(idUser))
			 {
				 JOptionPane.showMessageDialog(null, "Você já solicitou participação neste evento.");
				 return;
			 }
			 JOptionPane.showInternalMessageDialog(null,"Pedido para participar enviado");
			 event.addPendingRequestIds(idUser);
		  }
		}else {
			JOptionPane.showMessageDialog(null, "Você já participa desse evento!");
		}
	}
	
	public void adicionarParticipantesPrivateEvento(int idEvent,int idUser) {
		Event event = findEventById(idEvent);
		event.addConfirmedParticipantIds(idUser);
		UserController userController = UserController.getInstance();
		var user = userController.findUserById(idUser);
		user.addListaEventos(idEvent);
		event.removePendingRequestIds(idUser);
	    JOptionPane.showInternalMessageDialog(null,"Presença confirmada");
	}
	
	public void removerParticipante(int idEvent,int idUser)
	{
		Event event = findEventById(idEvent);
		event.removeConfirmedParticipantIds(idUser);
		UserController userController = UserController.getInstance();
		var user = userController.findUserById(idUser);
		user.removeListaEventos(idEvent);
	    JOptionPane.showInternalMessageDialog(null,"Participação cancelada");
	}
	
	public void negarParticipantesPrivateEvento(int idEvent,int idUser) {
		Event event = findEventById(idEvent);
		event.removePendingRequestIds(idUser);
	    JOptionPane.showInternalMessageDialog(null,"Usuario removida da lista de pedidos!");
	}

	public void print(List<Event> eventos) {
		for (Event eb : eventos) {
			System.out.println(eb.getId());
			System.out.println(eb.getTitle());
			System.out.println(eb.getDescription());
			System.out.println(eb.getType().getLabel());
			System.out.println(eb.getDate().toString());
			System.out.println(eb.getEndHours().toString());
			System.out.println(eb.getStartHours().toString());
			Cep cep = new Cep();
			cep = eb.getCep();
			System.out.println(cep.getLocality());
			System.out.println(cep.getState());
			System.out.println("---------------------------");
		}

	}

	public void print() {
		for (Event eb : eventController) {
			System.out.println(eb.getId());
			System.out.println(eb.getTitle());
			System.out.println(eb.getDescription());
			System.out.println(eb.getType().getLabel());
			System.out.println(eb.getDate().toString());
			System.out.println(eb.getEndHours().toString());
			System.out.println(eb.getStartHours().toString());
			Cep cep = new Cep();
			cep = eb.getCep();
			System.out.println(cep.getLocality());
			System.out.println(cep.getState());
			System.out.println("---------------------------");
		}

	}
	public void ConfirmarPresenca(int idEvento, int idUser) {
		var event = findEventById(idEvento); 
		event.addParticipaoConfirmada(idUser);
		var userController = UserController.getInstance();
		var user = userController.findUserById(idUser);
		user.addParticipaoConfirmada(idEvento);
		JOptionPane.showMessageDialog(null, "Presença Confirmada");
	}
	
	public void negarPresenca(int idEvento, int idUser)
	{
		var event = findEventById(idEvento); 
		
	}

	public void filterCategories(List<EventClassification> eventClassification) {
		List<Event> eventos = eventController.find(event -> eventClassification.contains(event.getType()))
				.collect(Collectors.toList());
		print(eventos);
	}

	public List<Event> getAllEvents() {
		return eventController.find(Event::isAtivo).toList();
	}

	public List<Event> filterEventByPesquisa(String pesquisa) {
		List<Event> eventos = eventController.find(event -> event.getTitle().toLowerCase().contains(pesquisa))
				.collect(Collectors.toList());
		return eventos;
	}

	public void createEventSemMessageBox(int idMaker, String title, String description, EventClassification type, String date,String dateEnd,
			String startHours, String endHours, String caminho, String cep, String estado, String cidade, String bairro,
			String rua, String numero, String complemento,Visibilidade visibilidade) {
		var e = new Event();
		var address = new Address();
		e.setTitle(title);
		e.setDescription(description);
		e.setType(type);
		e.setDate(date);
		e.setDateEnd(dateEnd);
		e.setStartHours(startHours);
		e.setEndHours(endHours);
		e.setImagePath(caminho);
		address.setCep(cep);
		address.setEstado(estado);
		address.setCidade(cidade);
		address.setBairro(bairro);
		address.setRua(rua);
		address.setNumero(numero);
		e.setVisibilidade(visibilidade);
		if (!complemento.isEmpty()) {
			address.setComplemento(complemento);
		}
		e.setAddress(address);
		if (!e.getTitle().isEmpty() && !e.getDescription().isEmpty() && !e.getDate().isEmpty() && !e.getStartHours().isEmpty()
				&& !e.getImagePath().isEmpty() && e.getAddress() != null) {
			e.setId(generateId());
			e.setIdMaker(idMaker);
			e.addConfirmedParticipantIds(idMaker);
			var userController = UserController.getInstance();
			var user = userController.findUserById(idMaker);
			user.addListMyEvents(e.getId());
			eventController.add(e);
		} else {
		}

	}
	public void adicionarParticipanteSemMessageBox(int idEvent, int idUser)
	{
		UserController userController = UserController.getInstance();
		Event event = findEventById(idEvent);
		User user = userController.findUserById(idUser);
		if(!event.usersParticipaOuNão(idUser))
		{
		 if(event.getVisibilidade() == Visibilidade.PUBLICO)
		 {
			 user.addListaEventos(idEvent);
			event.addConfirmedParticipantIds(idUser);
		 }else
		  {
			 if(event.usersParticipaOuNãoListPending(idUser))
			 {
				 return;
			 }
			 event.addPendingRequestIds(idUser);
		  }
		}else {
			JOptionPane.showMessageDialog(null, "Você já participa desse evento!");
		}
	}
	
	public void ConfirmarPresencaSemMessageBox(int idEvento, int idUser) {
		var event = findEventById(idEvento); 
		event.addParticipaoConfirmada(idUser);
		var userController = UserController.getInstance();
		var user = userController.findUserById(idUser);
		user.addParticipaoConfirmada(idEvento);
	}

	public static int generateId() {
		return lastGeneratedId++;
	}
}
