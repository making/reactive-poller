package am.ik.jdt;

import static java.util.regex.Pattern.quote;

public class Data {
	private String question;
	private String answer;
	private Long count;

	Data() {
	}

	public Data(String question, String answer) {
		this(question, answer, 0L);
	}

	public Data(String question, String answer, Long count) {
		this.question = question;
		this.answer = answer;
		this.count = count;
	}

	public static Data fromKeyAndCount(String key, Long count) {
		String[] ks = key.split(quote(":"));
		return new Data(ks[0], ks[1], count);
	}

	public String key() {
		return this.question + ":" + this.answer;
	}

	public Data withCount(Long count) {
		return new Data(this.question, this.answer, count);
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return "Data{" + "question='" + question + '\'' + ", answer='" + answer + '\''
				+ ", count=" + count + '}';
	}
}
