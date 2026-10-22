import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import CheckForm from './CheckForm';
import { strings as t } from '../i18n/strings';

function renderForm(props = {}) {
  const formProps = {
    t: t.en,
    busy: null,
    apiError: '',
    onAnalyzeText: vi.fn(),
    onAnalyzeImage: vi.fn(),
    ...props,
  };
  const view = render(<CheckForm {...formProps} />);
  return { formProps, ...view };
}

/** The submit button inside the message form (first of the two "Check It Now" buttons). */
function submitButton() {
  return screen.getAllByRole('button', { name: t.en.analyzeButton })[0];
}

describe('CheckForm', () => {
  it('rejects an empty message with a clear validation message', () => {
    const { formProps } = renderForm();
    fireEvent.click(submitButton());
    expect(screen.getByText(t.en.emptyInputError)).toBeInTheDocument();
    expect(formProps.onAnalyzeText).not.toHaveBeenCalled();
  });

  it('submits the typed message (trimmed) via the callback', () => {
    const { formProps } = renderForm();
    fireEvent.change(screen.getByLabelText(t.en.inputLabel), {
      target: { value: '  Your KYC has expired  ' },
    });
    fireEvent.click(submitButton());
    expect(formProps.onAnalyzeText).toHaveBeenCalledWith('Your KYC has expired');
  });

  it('shows a safe Try Again action for API errors and re-submits the input', () => {
    const { formProps } = renderForm({ apiError: t.en.networkError });
    fireEvent.change(screen.getByLabelText(t.en.inputLabel), {
      target: { value: 'URGENT: verify your account' },
    });
    expect(screen.getByText(t.en.networkError)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Try Again/ }));
    expect(formProps.onAnalyzeText).toHaveBeenCalledWith('URGENT: verify your account');
  });

  it('does not let the user request an image analysis without choosing a file', () => {
    const { formProps } = renderForm();
    const imageButtons = screen.getAllByRole('button', { name: t.en.analyzeButton });
    // second "Check It Now" belongs to the image upload panel
    fireEvent.click(imageButtons[1]);
    expect(screen.getByText(t.en.chooseFileFirst)).toBeInTheDocument();
    expect(formProps.onAnalyzeImage).not.toHaveBeenCalled();
  });
});