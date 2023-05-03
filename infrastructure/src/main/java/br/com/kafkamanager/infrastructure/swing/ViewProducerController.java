package br.com.kafkamanager.infrastructure.swing;

import br.com.kafkamanager.application.message.create.CreateMessageCommand;
import br.com.kafkamanager.application.message.create.CreateMessageUseCase;
import br.com.kafkamanager.application.topic.list.ListTopicUseCase;
import br.com.kafkamanager.domain.exceptions.NotificationException;
import br.com.kafkamanager.infrastructure.swing.util.HeaderParser;
import br.com.kafkamanager.infrastructure.swing.util.JFileChooserUtil;
import br.com.kafkamanager.infrastructure.swing.util.JOptionUtil;
import br.com.kafkamanager.infrastructure.util.ContextUtil;
import br.com.kafkamanager.infrastructure.util.JsonUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class ViewProducerController extends ViewProducer {

    private static final String[] COLUMN_NAMES = {"Key", "Value"};
    private static final String COULD_NOT_LOAD_FILE_MESSAGE = "Could not load the file";
    private static final String FILE_SAVED_SUCCESSFULLY_MESSAGE = "A File saved successfully!";
    private static final String COULD_NOT_SAVE_FILE_MESSAGE = "Could not save the file!";
    private static final String MESSAGE_CREATED_SUCCESSFULLY = "Message created successfully!";
    private static final String MESSAGE_CREATED_ERROR = "Could not create the message";

    private final ListTopicUseCase listTopicUseCase;
    private final CreateMessageUseCase createMessageUseCase;
    private final String topicName;
    private DefaultTableModel model;

    public ViewProducerController(String topicName) {
        super();
        listTopicUseCase = ContextUtil.getBean(ListTopicUseCase.class);
        createMessageUseCase = ContextUtil.getBean(CreateMessageUseCase.class);
        this.topicName = topicName;
        start();
        setVisible(true);
    }

    private void start() {
        initializeTopics();
        initializeListeners();
        createTable();
        model.addRow(new Object[]{"", ""});
    }

    private void initializeTopics() {
        final var topics = new TreeSet<>(listTopicUseCase.execute());
        for (final var kafkaTopicDto : topics) {
            comboTopic.addItem(kafkaTopicDto.getId().getValue());
        }
        comboTopic.setSelectedItem(topicName);
    }

    private void initializeListeners() {
        btnClose.addActionListener(e -> dispose());
        btnProduce.addActionListener(e -> producer());
        btnLoadProducer.addActionListener(e -> load());
        btnSave.addActionListener(e -> save());
        btnPlus.addActionListener(e -> plus());
        btnSubtract.addActionListener(e -> subtract());
        btnImportHeaderKafdrop.addActionListener(e -> importHeadersFromKafdrop());
        btnFormatter.addActionListener(e -> txtValue.setText(JsonUtil.format(txtValue.getText())));
    }


    private void createTable() {
        model = new DefaultTableModel(COLUMN_NAMES, 0);
        table.setModel(model);
    }

    private void producer() {
        try {
            createMessageUseCase.execute(buildMessageCommand());
            JOptionPane.showMessageDialog(null, MESSAGE_CREATED_SUCCESSFULLY);
        } catch (NotificationException e) {
            JOptionPane.showMessageDialog(null,
                MESSAGE_CREATED_ERROR + ": " + e.getFormattedErrors());
        }
    }

    private void load() {
        final var file = JFileChooserUtil.chooseFile();
        if (file == null) {
            return;
        }
        try {
            final var kafkaProducerDto = JsonUtil.readJsonFile(file, CreateMessageCommand.class);
            comboTopic.setSelectedItem(kafkaProducerDto.getTopicName());
            txtKey.setText(kafkaProducerDto.getKey());
            txtValue.setText(kafkaProducerDto.getMessage());
            populateTableWithHeaders(kafkaProducerDto.getHeaders());
        } catch (FileNotFoundException e) {
            showMessageDialog(COULD_NOT_LOAD_FILE_MESSAGE);
        }
    }

    private void save() {
        final var file = JFileChooserUtil.chooseFile();
        if (file == null) {
            return;
        }
        try {
            JsonUtil.writeJsonFile(buildMessageCommand(), file);
            showMessageDialog(FILE_SAVED_SUCCESSFULLY_MESSAGE);
        } catch (IOException e) {
            showMessageDialog(COULD_NOT_SAVE_FILE_MESSAGE);
        }
    }

    private void plus() {
        model.addRow(new Object[]{"", ""});
    }

    private void subtract() {
        model.removeRow(table.getSelectedRow());
    }

    private CreateMessageCommand buildMessageCommand() {
        final var map = new HashMap<String, String>();
        final var totalRow = table.getRowCount();
        for (int i = 0; i < totalRow; i++) {
            String key = table.getValueAt(i, 0).toString().strip();
            String value = table.getValueAt(i, 1).toString().strip();
            if (!value.isBlank()) {
                map.put(key, value);
            }
        }
        return CreateMessageCommand.of(txtKey.getText().strip(),
            String.valueOf(comboTopic.getSelectedItem()),
            txtValue.getText().strip(), map);
    }

    private void importHeadersFromKafdrop() {
        final var header = JOptionUtil.showCustomInputDialog("Copy headers from Kafdrop:",
            "Complete");
        if (header == null || header.isBlank()) {
            return;
        }
        populateTableWithHeaders(HeaderParser.parseEventString(header));
    }

    private void populateTableWithHeaders(Map<String, String> headerMap) {
        createTable();
        for (final var entry : headerMap.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
    }

    private void showMessageDialog(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
