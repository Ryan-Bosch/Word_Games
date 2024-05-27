package com.boschryan.wordgames;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private EditText mFocusET;
    private char[] letters;
    private LetterState[] states;
    private int numChars;
    private int currentIndex = 0;
    private int currentLine = 1;
    private LetterAdapter letterAdapter;
    private HashSet<String> wordSet;
    private String goal;
    private String mPreviousText;
    private TextWatcher textWatcher;

    public enum LetterState {
        CORRECT,
        PARTIALLY_CORRECT,
        INCORRECT,
        NULL
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Intent intent = getIntent();
        numChars = intent.getIntExtra("EXTRA_SELECTION", 5);

        RecyclerView recyclerView = findViewById(R.id.game_view);
        RecyclerView.LayoutManager gridLayoutManager =
                new GridLayoutManager(getApplicationContext(), numChars);
        recyclerView.setLayoutManager(gridLayoutManager);

        letters = new char[numChars*(numChars+1)];
        states = new LetterState[numChars*(numChars+1)];
        Arrays.fill(states, LetterState.NULL);

        letterAdapter = new LetterAdapter(letters, states);
        recyclerView.setAdapter(letterAdapter);


        // Load the words from the online library
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadWordsFromLibrary();
            }
        }).start();

        // Open the soft keyboard
        mFocusET = findViewById(R.id.et_focus);
        mFocusET.requestFocus();
        WindowCompat.getInsetsController(getWindow(), mFocusET).show(WindowInsetsCompat.Type.ime());


        //Add event handler for typing
        mFocusET.addTextChangedListener(textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            /** @noinspection DataFlowIssue*/
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void afterTextChanged(Editable s) {
                String currentText = s.toString();

                // If the user pressed the Enter key
                if (currentText.endsWith("\n")) {
                    currentText = mPreviousText; // Remove the newline character
                    mFocusET.setText(mPreviousText);

                    // If current line is full
                    if (currentText.length() == numChars * currentLine) {
                        String currentWord = currentText.substring(numChars*(currentLine-1), numChars*currentLine);
                        if (wordSet.contains(currentWord.toLowerCase())) {
                            int correctLetters = 0;

                            // Count occurrences of each character in goal
                            HashMap<Character, Integer> goalCounts = new HashMap<>();
                            for (char c : goal.toCharArray()) {
                                if (!goalCounts.containsKey(c)) {
                                    goalCounts.put(c, 1);
                                } else {
                                    goalCounts.put(c, goalCounts.get(c) + 1);
                                }
                            }

                            // Loop to mark CORRECT letters first
                            for (int i = 0, j = currentIndex - numChars; i < numChars; i++, j++) {
                                char c = currentWord.charAt(i);

                                // Correct location and count not depleted
                                if (c == goal.charAt(i)) {
                                    states[j] = LetterState.CORRECT;
                                    correctLetters++;

                                    // Decrement count of this character in goal
                                    goalCounts.put(c, goalCounts.get(c) - 1);

                                    letterAdapter.notifyItemChanged(j);
                                }
                            }

                            // Second loop: mark PARTIAL or INCORRECT letters based on remaining counts
                            for (int i = 0, j = currentIndex - numChars; i < numChars; i++, j++) {
                                // If already marked as CORRECT, skip it
                                if (states[j].equals(LetterState.CORRECT)) continue;

                                char c = currentWord.charAt(i);

                                // Incorrect letter
                                if (!goalCounts.containsKey(c) || goalCounts.get(c) <= 0)
                                    states[j] = LetterState.INCORRECT;

                                // Partially correct
                                else if (goal.indexOf(c) != -1 && goalCounts.get(c) > 0) {
                                    states[j] = LetterState.PARTIALLY_CORRECT;
                                    goalCounts.put(c, goalCounts.get(c) - 1);
                                }

                                letterAdapter.notifyItemChanged(j);
                            }

                            /*
                            for (int i = 0, j = currentIndex - numChars; i < numChars; i++, j++) {
                                char c = currentWord.charAt(i);

                                // Correct location
                                if (c == goal.charAt(i)) {
                                    states[j] = LetterState.CORRECT;
                                    correctLetters++;

                                    // Correct letter, incorrect location
                                } else if (goal.indexOf(c) != -1) {
                                    states[j] = LetterState.PARTIALLY_CORRECT;

                                    // Incorrect letter
                                } else {
                                    states[j] = LetterState.INCORRECT;
                                }
                                letterAdapter.notifyItemChanged(i);
                            }*/

                            currentLine++;
                            if (correctLetters == numChars) {
                                // User wins
                                // Add code here to handle the win condition
                                createWinMessage(true);
                                mFocusET.removeTextChangedListener(textWatcher);
                            } else if (currentLine > numChars + 1) {
                                // User loses
                                // Add code here to handle the lose condition
                                createWinMessage(false);
                                mFocusET.removeTextChangedListener(textWatcher);
                            }
                        } else {
                            new AlertDialog.Builder(GameActivity.this)
                                    .setMessage("Word not found in dictionary")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                    mFocusET.setSelection(currentIndex);

                // If the current line is filled
                } else if (currentText.length() > numChars * currentLine) {
                    mFocusET.setText(currentText.substring(0, numChars * currentLine));
                    mFocusET.setSelection(numChars * currentLine);

                // If the user deleted a character
                } else if (currentText.length() < currentIndex) {
                    if (currentIndex <= numChars * (currentLine-1)) {
                        // Add the deleted character back
                        currentText = mPreviousText;
                        mFocusET.setText(currentText);
                        mFocusET.setSelection(currentIndex);
                    } else {
                        currentIndex--;
                        letters[currentIndex] = '\u0000';
                        letterAdapter.notifyItemChanged(currentIndex);
                    }

                // If the user added a character
                } else if (currentText.length() == currentIndex+1) {
                    char c = currentText.charAt(currentIndex);

                    // If the character is valid
                    if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
                        letters[currentIndex] = c;
                        letterAdapter.notifyItemChanged(currentIndex);
                        currentIndex++;
                    } else {
                        mFocusET.setText(currentText.substring(0, currentIndex));
                        mFocusET.setSelection(currentIndex);
                    }
                }
                mPreviousText = currentText;
            }
        });
    }

    private void loadWordsFromLibrary() {
        try {
            wordSet = new HashSet<String>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("wordlist.txt")));

            String word;
            while ((word = reader.readLine()) != null) {
                if (word.length() == numChars) {
                    wordSet.add(word);
                }
            }

            reader.close();

            if (!wordSet.isEmpty()) {
                Random random = new Random();
                goal = (String) wordSet.toArray()[random.nextInt(wordSet.size())];

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView test = findViewById(R.id.TestTV);
                        test.setText(goal);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createWinMessage(boolean win) {
        TextView textView = new TextView(this);
        textView.setText(goal);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://www.merriam-webster.com/dictionary/" + goal);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Result")
                .setMessage("You " + (win ? "won" : "lost") + "! The word was: ")
                .setView(textView)
                .setPositiveButton("OK", null)
                .create()
                .show();
    }

    /** @noinspection InnerClassMayBeStatic*/
    private class LetterHolder extends RecyclerView.ViewHolder {
        /** @noinspection FieldMayBeFinal*/
        private TextView letterView;

        public LetterHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.recycler_view_item, parent, false));
            letterView = itemView.findViewById(R.id.letterTextView);
            letterView.setOnClickListener(view -> {
                mFocusET.requestFocus();
                WindowCompat.getInsetsController(getWindow(), mFocusET).show(WindowInsetsCompat.Type.ime());
            });
        }

        public void bind(char letter, LetterState state) {
            letterView.setText(String.valueOf(letter));

            switch (state) {
                case CORRECT:
                    letterView.setBackgroundResource(R.drawable.correct_back);
                    break;
                case PARTIALLY_CORRECT:
                    letterView.setBackgroundResource(R.drawable.partially_correct_back);
                    break;
                case INCORRECT:
                    letterView.setBackgroundResource(R.drawable.incorrect_back);
                    break;
                default:
                    letterView.setBackgroundResource(R.drawable.null_back);
            }
        }
    }

    private class LetterAdapter extends RecyclerView.Adapter<LetterHolder> {
        /** @noinspection FieldMayBeFinal*/
        private char[] letterList;
        /** @noinspection FieldMayBeFinal*/
        private LetterState[] stateList;

        public LetterAdapter(char[] letters, LetterState[] states) {
            letterList = letters;
            stateList = states;
        }

        @NonNull
        @Override
        public LetterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
            return new LetterHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(LetterHolder holder, int position) {
            holder.bind(letterList[position], stateList[position]);
        }

        @Override
        public int getItemCount() {
            return letterList.length;
        }
    }
}
